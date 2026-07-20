package com.gtdflow.widget.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Планирование обновления виджетов: периодика раз в 30 минут (минимальный интервал
 * WorkManager — 15 мин; берём 30 как компромисс свежесть/батарея) и разовый
 * немедленный пересчёт по интеракции (тап, переключение чекбокса, захват, выбор vault).
 */
object RefreshScheduler {

    private const val PERIODIC_NAME = "gtd_widgets_periodic"
    private const val ONESHOT_NAME = "gtd_widgets_refresh_now"
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

    /** Разовый немедленный пересчёт (REPLACE — свежий запрос вытесняет ожидающий). */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
