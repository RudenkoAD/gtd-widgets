package com.gtdflow.widget.engine

import android.content.Context
import com.gtdflow.widget.perf.Perf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Запуск блока с движком QuickJS на ВЫДЕЛЕННОМ однопоточном диспетчере,
 * с КЭШЕМ движка на процесс.
 *
 * Контекст QuickJS привязан к потоку создания и не потокобезопасен. Однопоточный
 * executor гарантирует, что и создание движка, и все его вызовы (даже сквозь
 * suspend-точки внутри block — корутина возвращается на тот же единственный поток)
 * идут с одного потока.
 *
 * КЭШ. Раньше на каждый вызов создавался свежий executor + контекст и заново
 * парсился бандл ядра (~85КБ) — это платилось на КАЖДОМ обновлении/захвате/правке,
 * причём захват гонял движок дважды. Теперь пара «executor + движок» живёт в
 * процессе и переиспользуется: бандл парсится один раз, повторные вызовы платят
 * только за сам расчёт. Ядро на каждый compute строит свежий индекс из input.files,
 * а мост обнуляет __gtdResult/__gtdError перед каждым запуском (см. widget-bridge.js)
 * — состояние между вызовами не течёт. Один живой контекст QuickJS для виджет-процесса
 * дешёв по памяти, поэтому никакого TTL: простейший корректный кэш.
 *
 * ИНВАЛИДАЦИЯ. Любой Throwable, вылетевший из вызова через движок (включая отмену
 * корутины: нельзя гарантировать, в каком состоянии блок оставил контекст), выбрасывает
 * закэшированную пару: контекст закрывается НА СВОЁМ потоке (задачей в executor перед
 * его shutdown), и следующий use() поднимет свежую. Заклинивший контекст не отравляет
 * последующие вызовы. Инвалидация потокобезопасна ([EngineCache]) — вызовы приходят
 * конкурентно из корутин AppScope и колбэков Glance-экшенов.
 */
object EngineRunner {

    /**
     * Пара «однопоточный executor + движок». Движок создаётся лениво ВНУТРИ потока
     * executor'а (контекст QuickJS обязан родиться на потоке своих вызовов), поэтому
     * поле [engine] читается/пишется только с этого потока — синхронизация не нужна.
     */
    private class Slot {
        val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "gtd-quickjs") }
        val dispatcher = executor.asCoroutineDispatcher()
        var engine: QuickJsEngine? = null

        /** Закрыть движок на его потоке и погасить executor. Зовётся один раз (кэшем). */
        fun dispose() {
            executor.execute {
                engine?.close()
                engine = null
            }
            executor.shutdown()
        }
    }

    private val cache = EngineCache(
        factory = { Slot() },
        onEvict = { it.dispose() },
    )

    suspend fun <T> use(context: Context, block: suspend (QuickJsEngine) -> T): T {
        val slot = cache.acquire()
        return try {
            runOn(slot, context, block)
        } catch (e: CancellationException) {
            // Если отменили НАС — честно пробрасываем отмену. Иначе это гонка: слот
            // успели инвалидировать и закрыть между acquire() и диспатчем (закрытый
            // dispatcher отменяет корутину). Одна повторная попытка на свежем слоте.
            currentCoroutineContext().ensureActive()
            runOn(cache.acquire(), context, block)
        }
    }

    /** Выполнить блок на потоке слота; любой Throwable инвалидирует слот и летит дальше. */
    private suspend fun <T> runOn(
        slot: Slot,
        context: Context,
        block: suspend (QuickJsEngine) -> T,
    ): T =
        try {
            withContext(slot.dispatcher) {
                val engine = slot.engine ?: Perf.span("engine.create") {
                    QuickJsEngine.create(context)
                }.also { slot.engine = it }
                block(engine)
            }
        } catch (e: Throwable) {
            if (cache.invalidate(slot)) Perf.mark("engine.invalidate reason=${e.javaClass.simpleName}")
            throw e
        }
}
