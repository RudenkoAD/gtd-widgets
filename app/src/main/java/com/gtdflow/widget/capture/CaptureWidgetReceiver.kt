package com.gtdflow.widget.capture

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver виджета «Захват». Периодику НЕ трогает: это чистая кнопка без данных
 * vault — ей нечего пересчитывать (в отличие от «Сегодня»/«Входящих»/«Агенды»/«Сейчас»).
 */
class CaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaptureWidget()
}
