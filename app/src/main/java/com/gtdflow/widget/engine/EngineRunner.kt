package com.gtdflow.widget.engine

import android.content.Context
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Запуск блока с движком QuickJS на ВЫДЕЛЕННОМ однопоточном диспетчере.
 *
 * Контекст QuickJS привязан к потоку создания и не потокобезопасен. Однопоточный
 * executor гарантирует, что и создание движка, и все его вызовы (даже сквозь
 * suspend-точки внутри block — корутина возвращается на тот же единственный поток)
 * идут с одного потока. Движок закрывается, диспетчер завершается после блока.
 */
object EngineRunner {
    suspend fun <T> use(context: Context, block: suspend (QuickJsEngine) -> T): T {
        val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "gtd-quickjs") }
        val dispatcher = executor.asCoroutineDispatcher()
        try {
            return withContext(dispatcher) {
                val engine = QuickJsEngine.create(context)
                try {
                    block(engine)
                } finally {
                    engine.close()
                }
            }
        } finally {
            dispatcher.close()
        }
    }
}
