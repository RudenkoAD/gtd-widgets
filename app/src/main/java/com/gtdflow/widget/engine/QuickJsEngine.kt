package com.gtdflow.widget.engine

import android.content.Context
import com.whl.quickjs.wrapper.QuickJSContext
import java.io.Closeable

/**
 * Обёртка встраиваемого движка QuickJS (wang.harlon.quickjs) над бандлом ядра
 * widget-core.js + мостом widget-bridge.js.
 *
 * ЖИЗНЕННЫЙ ЦИКЛ И ПОТОК: контекст QuickJS привязан к потоку создания и НЕ
 * потокобезопасен. Создавайте движок и вызывайте его методы в ОДНОМ синхронном
 * блоке на фоновом потоке (без suspension-точек между вызовами), затем close().
 * Один экземпляр допускает несколько compute()/build*(): бандл парсится один раз,
 * ядро на каждый вызов строит свежий индекс из input.files (без утечки состояния
 * между вызовами; мост обнуляет __gtdResult/__gtdError перед каждым запуском).
 *
 * ПРОМИСЫ: computeWidgetData асинхронна, но ждёт только микрозадачи. Обёртка после
 * каждого call() прогоняет JS_ExecutePendingJob до опустошения очереди — поэтому к
 * возврату из __gtdRunCompute результат уже записан в глобал, и __gtdReadCompute
 * читает его синхронно. См. widget-bridge.js.
 */
class QuickJsEngine private constructor(
    private val ctx: QuickJSContext,
) : Closeable {

    /**
     * Вызвать ядро и вернуть разобранный WidgetData. Бросает [EngineException] при
     * ошибке ядра (errors[] внутри WidgetData НЕ бросают — это штатные диагностики
     * секций, доступны в результате).
     */
    fun compute(input: WidgetInput): WidgetData {
        callVoid("__gtdRunCompute", input.toJson())
        val raw = callString("__gtdReadCompute")
        return when {
            raw.startsWith("OK:") -> WidgetJson.decodeFromString(WidgetData.serializer(), raw.substring(3))
            raw.startsWith("ERR:") -> throw EngineException(raw.substring(4))
            else -> throw EngineException("unexpected engine reply: ${raw.take(64)}")
        }
    }

    /** Синхронный buildCaptureLine ядра. Бросает [EngineException] при пустом тексте. */
    fun buildCaptureLine(text: String, location: String?): String =
        try {
            callString("__gtdBuildCapture", text, location)
        } catch (e: Exception) {
            throw EngineException("buildCaptureLine failed: ${e.message}", e)
        }

    /** Синхронный captureTargetPath ядра — путь файла входящих пространства. */
    fun captureTargetPath(dataJson: String?, namespace: String?): String =
        callString("__gtdCaptureTarget", dataJson, namespace)

    // --- низкоуровневые вызовы моста ---

    private fun callVoid(fn: String, vararg args: Any?) {
        val global = ctx.globalObject
        val f = global.getJSFunction(fn) ?: throw EngineException("bridge fn '$fn' missing")
        try {
            f.call(*args)
        } finally {
            f.release()
        }
    }

    private fun callString(fn: String, vararg args: Any?): String {
        val global = ctx.globalObject
        val f = global.getJSFunction(fn) ?: throw EngineException("bridge fn '$fn' missing")
        try {
            return f.call(*args) as? String
                ?: throw EngineException("bridge fn '$fn' returned non-string")
        } finally {
            f.release()
        }
    }

    override fun close() {
        try {
            ctx.destroy()
        } catch (_: Exception) {
            // движок уже мог быть освобождён — закрытие идемпотентно на нашей стороне
        }
    }

    class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        private const val CORE_ASSET = "widget-core.js"
        private const val BRIDGE_ASSET = "widget-bridge.js"

        /**
         * Создать движок: поднять контекст QuickJS и загрузить бандл ядра + мост.
         * Вызывать на том же потоке, где будут вызовы движка.
         */
        fun create(context: Context): QuickJsEngine {
            val core = readAsset(context, CORE_ASSET)
            val bridge = readAsset(context, BRIDGE_ASSET)
            val ctx = QuickJSContext.create()
            try {
                ctx.evaluate(core)
                ctx.evaluate(bridge)
            } catch (e: Exception) {
                ctx.destroy()
                throw EngineException("engine init failed: ${e.message}", e)
            }
            return QuickJsEngine(ctx)
        }

        private fun readAsset(context: Context, name: String): String =
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
