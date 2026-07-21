package com.gtdflow.widget.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Срабатывание будильника напоминания по времени (AlarmManager → сюда). Достаёт из
 * экстр заголовок/текст/идентификатор и показывает уведомление. Вся тяжёлая работа
 * (расчёт плана) сделана заранее при перепланировании — здесь только показ.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        ReminderNotifier.notifyTime(context.applicationContext, id, title, text)
    }

    companion object {
        const val ACTION_FIRE = "com.gtdflow.widget.reminders.ACTION_FIRE"
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
