package com.gtdflow.widget.agenda

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.gtdflow.widget.work.RefreshScheduler

/** BroadcastReceiver виджета «Агенда». При первом экземпляре — планируем периодику. */
class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshScheduler.ensurePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Последний экземпляр этого типа снят — периодика нужна, только если остались другие.
        RefreshScheduler.cancelPeriodicIfNoWidgets(context)
    }
}
