package com.gtdflow.widget.nownext

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gtdflow.widget.work.AppScope
import com.gtdflow.widget.work.WidgetService
import kotlinx.coroutines.launch

/**
 * Срабатывание аларма границы «Сейчас → Далее» (см. [NowNextAlarms]). Делает обычный
 * пересчёт виджетов — тот же путь, что периодика/интеракция: он перерисует «Сейчас» под
 * новую хронологию И перепланирует уже следующую границу.
 *
 * goAsync держит ресивер живым на время пересчёта; работа — в процессной [AppScope]
 * (переживёт возврат из onReceive), finish() по завершении.
 */
class NowNextAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val app = context.applicationContext
        val pending = goAsync()
        AppScope.scope.launch {
            try {
                WidgetService.refreshCoalesced(app)
            } catch (t: Throwable) {
                Log.d(TAG, "nownext boundary refresh failed: ${t.message}")
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        private const val TAG = "GtdWidget"
        const val ACTION_FIRE = "com.gtdflow.widget.nownext.ACTION_BOUNDARY"
    }
}
