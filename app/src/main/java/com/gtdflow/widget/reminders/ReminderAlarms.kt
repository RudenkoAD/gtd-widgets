package com.gtdflow.widget.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Планирование напоминаний по времени через AlarmManager — WHOLESALE.
 *
 * На каждый пересчёт: сравниваем сохранённые requestCode с новым планом, отменяем
 * «ушедшие», (пере)ставим актуальные (FLAG_UPDATE_CURRENT перезапишет экстры), затем
 * сохраняем новый набор кодов. Точность: setExactAndAllowWhileIdle при праве на точные
 * будильники (canScheduleExactAlarms), иначе setWindow(±5 мин) — тоже проходит Doze.
 *
 * requestCode детерминирован ([ReminderRequestCode]) — переживает перезапуск процесса и
 * BOOT-перевзвод, поэтому отмена/перезапись находят «свой» PendingIntent.
 */
object ReminderAlarms {

    private const val TAG = "GtdRem"
    private const val WINDOW_MS = 5 * 60 * 1000L // ±5 минут для неточного окна

    /** Привести запланированные будильники к [plan] (пустой список = отменить все). */
    suspend fun reschedule(context: Context, plan: List<PlannedReminder>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val oldCodes = ReminderStore.scheduledCodes(context)
        val newCodes = plan.mapTo(HashSet()) { it.requestCode }

        // Нечего отменять и нечего ставить — не пишем в DataStore зря (частый пересчёт).
        if (oldCodes.isEmpty() && newCodes.isEmpty()) return

        for (code in oldCodes) {
            if (code !in newCodes) cancel(context, am, code)
        }
        for (r in plan) {
            schedule(context, am, r)
        }
        ReminderStore.saveScheduledCodes(context, newCodes)
        Log.d(TAG, "alarms scheduled=${plan.size} cancelled=${(oldCodes - newCodes).size}")
    }

    private fun schedule(context: Context, am: AlarmManager, r: PlannedReminder) {
        val pi = firePendingIntent(
            context, r.requestCode, r.title, r.text,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.triggerAtMillis, pi)
        } else {
            am.setWindow(
                AlarmManager.RTC_WAKEUP,
                r.triggerAtMillis - WINDOW_MS,
                2 * WINDOW_MS,
                pi,
            )
        }
    }

    private fun cancel(context: Context, am: AlarmManager, code: Int) {
        // Для отмены достаточно совпадения action+компонента+requestCode (экстры не влияют
        // на сопоставление PendingIntent). FLAG_NO_CREATE — не плодим лишний intent.
        val pi = firePendingIntent(
            context, code, title = "", text = "",
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun firePendingIntent(
        context: Context,
        requestCode: Int,
        title: String,
        text: String,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_ID, requestCode)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
