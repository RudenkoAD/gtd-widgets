package com.gtdflow.widget.reminders

import android.content.Context
import android.util.Log
import java.time.ZoneId

/**
 * Оркестратор перепланирования напоминаний — зовётся после КАЖДОГО успешного пересчёта
 * виджетов (см. WidgetService) и из [BootReceiver].
 *
 * По прочтении настроек:
 *  • по времени включено → строим план на горизонт ~36 ч и перепланируем будильники
 *    wholesale (отменяем ушедшие); выключено → снимаем все;
 *  • по месту включено и есть разрешения → перевооружаем геофенсы; иначе снимаем.
 *
 * Кандидаты приходят готовыми из движка (агенда на несколько дней). Снимок времени
 * (сейчас/зона/сегодня) берётся здесь — чистый планировщик получает их явно.
 */
object ReminderScheduler {

    private const val TAG = "GtdRem"
    private const val HORIZON_HOURS = 36L
    private const val HORIZON_MILLIS = HORIZON_HOURS * 60L * 60L * 1000L

    suspend fun onRefresh(context: Context, candidates: List<ReminderCandidate>, todayIso: String) {
        val app = context.applicationContext
        val prefs = ReminderStore.prefs(app)
        ReminderNotifier.ensureChannel(app)

        // --- По времени ---
        if (prefs.timeEnabled) {
            val plan = ReminderPlan.plan(
                candidates = candidates,
                leadMinutes = prefs.leadMinutes,
                nowMillis = System.currentTimeMillis(),
                zone = ZoneId.systemDefault(),
                horizonMillis = HORIZON_MILLIS,
            )
            ReminderAlarms.reschedule(app, plan)
        } else {
            ReminderAlarms.reschedule(app, emptyList()) // снять все
        }

        // --- По месту ---
        if (prefs.placeEnabled && GeofenceManager.hasPermissions(app)) {
            GeofenceManager.rearm(app, candidates, todayIso)
        } else {
            GeofenceManager.clear(app)
            if (prefs.placeEnabled) {
                Log.d(TAG, "geofence off: permission missing despite toggle")
            }
        }
    }
}
