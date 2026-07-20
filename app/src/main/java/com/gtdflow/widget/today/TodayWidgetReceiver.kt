package com.gtdflow.widget.today

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.gtdflow.widget.work.RefreshScheduler

/** BroadcastReceiver виджета «Сегодня». При добавлении первого экземпляра — планируем периодику. */
class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onEnabled(context: android.content.Context) {
        super.onEnabled(context)
        RefreshScheduler.ensurePeriodic(context)
    }

    override fun onDisabled(context: android.content.Context) {
        super.onDisabled(context)
        // Последний экземпляр этого типа снят — периодика нужна, только если остались другие.
        RefreshScheduler.cancelPeriodicIfNoWidgets(context)
    }
}
