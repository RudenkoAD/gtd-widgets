package com.gtdflow.widget.perf

import android.os.SystemClock
import android.util.Log

/**
 * Лёгкие метки производительности пути «действие в виджете → обновление UI».
 *
 * Всё пишется в logcat под тегом [TAG] = "GtdPerf". `Log.d` дёшев (форматирование
 * строки — единственная цена, и она незначима на фоне SAF-IO/QuickJS), поэтому
 * метки остаются в коде постоянно: они — штатный инструмент диагностики лагов
 * виджета на реальном устройстве, где JVM-профайлер недоступен.
 *
 * Как снимать замеры — см. DEV.md, раздел «Профилирование GtdPerf».
 *
 * Соглашение по меткам одного сценария (чекбокс «Входящие»):
 *  • toggle.received      — экшен получен (тап пришёл в процесс);
 *  • toggle.write         — запись `- [x]` в файл (took=…);
 *  • toggle.optimistic    — оптимистичное удаление строки из state + updateAll (took=…);
 *  • refresh.start        — старт полного пересчёта (inline);
 *  • scan …               — обход vault (files/dirs/enumMs/readMs/cacheHits/cacheReads);
 *  • engine.compute       — один вызов QuickJS (took=…);
 *  • refresh.update       — updateAll всех виджетов после пересчёта (took=…);
 *  • refresh.done         — пересчёт завершён (took=… от refresh.start).
 */
object Perf {
    const val TAG = "GtdPerf"

    /** Монотонные часы (мс) — устойчивы к переводу системного времени/сна. */
    fun nowMs(): Long = SystemClock.elapsedRealtime()

    /** Точечная метка с абсолютным временем (для меж-компонентных дельт из logcat). */
    fun mark(msg: String) {
        Log.d(TAG, "$msg @${nowMs()}ms")
    }

    /** Замер длительности блока: лог "label took=<ms>ms<extra>". Возвращает результат блока. */
    inline fun <T> span(label: String, extra: String = "", block: () -> T): T {
        val t0 = nowMs()
        val r = block()
        Log.d(TAG, "$label took=${nowMs() - t0}ms$extra")
        return r
    }
}
