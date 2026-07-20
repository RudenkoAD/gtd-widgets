package com.gtdflow.widget.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gtdflow.widget.perf.Perf
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Планирование обновления виджетов.
 *
 * ПЕРИОДИКА — раз в 30 минут через WorkManager (минимальный интервал — 15 мин; берём
 * 30 как компромисс свежесть/батарея). Это ЕДИНСТВЕННЫЙ путь, где остаётся WorkManager.
 *
 * НЕМЕДЛЕННЫЙ пересчёт по интеракции ([refreshNow]) больше НЕ идёт через WorkManager:
 * его латентность постановки — сотни мс (см. метки GtdPerf «workmanager.enqueue →
 * worker.start»), а Doze/батарейные политики могут отложить запуск ещё сильнее. Вместо
 * этого запускаем пересчёт инлайн в процессной [AppScope] (переживает закрытие
 * оверлея/завершение goAsync) со слиянием ([WidgetService.refreshCoalesced]).
 */
object RefreshScheduler {

    private const val PERIODIC_NAME = "gtd_widgets_periodic"
    private const val PERIOD_MINUTES = 30L

    /** Поставить периодику (идемпотентно — KEEP не пересоздаёт уже запланированную). */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            PERIOD_MINUTES, TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Немедленный пересчёт инлайн (без WorkManager), со слиянием. Fire-and-forget в
     * [AppScope]: корутина переживёт закрытие Activity/оверлея. Вызывающие, которым
     * важно ДОЖДАТЬСЯ пересчёта в своём же suspend-контексте (напр. Glance-экшен под
     * goAsync), могут звать [WidgetService.refreshCoalesced] напрямую.
     */
    fun refreshNow(context: Context) {
        Perf.mark("refresh.trigger")
        val app = context.applicationContext
        AppScope.scope.launch { WidgetService.refreshCoalesced(app) }
    }
}
