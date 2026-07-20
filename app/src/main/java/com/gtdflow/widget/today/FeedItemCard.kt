package com.gtdflow.widget.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodayItem
import com.gtdflow.widget.ui.EventSheetActivity

/**
 * Карточка элемента ленты «Сегодня»/«Агенды» (общая): слева — колонка ВРЕМЕНИ В ОДНУ
 * СТРОКУ (не переносится, фикс-ширина по «00:00–00:00»), справа — название и 📍 место.
 * Тап открывает шторку деталей [EventSheetActivity] для дня [dayIso]. [highlight] —
 * подсветка «текущего часа» (только «Сегодня»).
 */
@Composable
fun FeedItemCard(
    item: TodayItem,
    dayIso: String,
    vaultName: String?,
    highlight: Boolean,
) {
    val bg: ColorProvider =
        if (highlight) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant
    val open = actionStartActivity(
        EventSheetActivity.intent(LocalContext.current, item, dayIso, vaultName),
    )
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(bg)
                .cornerRadius(10.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(open),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Колонка времени: одна строка, фиксированная ширина под «00:00–00:00».
            Column(modifier = GlanceModifier.width(TIME_COLUMN_WIDTH)) {
                Text(
                    text = timeLabel(item),
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
                )
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = (if (item.isEvent) "• " else "") + item.title,
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                    maxLines = 2,
                )
                if (!item.location.isNullOrBlank()) {
                    Text(
                        text = "📍 ${item.location}",
                        style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Ширина колонки времени: вмещает «00:00–00:00» одной строкой без переноса. */
private val TIME_COLUMN_WIDTH = 76.dp

/** Одна строка времени: «весь день» / «HH:mm» / «HH:mm–HH:mm». */
private fun timeLabel(item: TodayItem): String =
    if (item.allDay || item.startMinutes == null) "весь день"
    else TimeUtil.formatRange(item.startMinutes, item.endMinutes)
