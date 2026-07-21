package com.gtdflow.widget.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gtdflow.widget.work.AppScope
import com.gtdflow.widget.work.RefreshScheduler
import com.gtdflow.widget.work.WidgetService
import kotlinx.coroutines.launch

/**
 * Перезагрузка телефона стирает будильники AlarmManager и геофенсы. После BOOT_COMPLETED
 * заново запускаем пересчёт (он в конце перевзводит напоминания через [ReminderScheduler])
 * и восстанавливаем периодику WorkManager на всякий случай.
 *
 * goAsync держит ресивер живым, пока идёт пересчёт; работа — в процессной [AppScope]
 * (переживёт возврат из onReceive), finish() вызывается по завершении.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val pending = goAsync()
        AppScope.scope.launch {
            try {
                RefreshScheduler.ensurePeriodic(app)
                WidgetService.refreshCoalesced(app)
            } catch (t: Throwable) {
                Log.d(TAG, "boot reschedule failed: ${t.message}")
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        private const val TAG = "GtdRem"
    }
}
