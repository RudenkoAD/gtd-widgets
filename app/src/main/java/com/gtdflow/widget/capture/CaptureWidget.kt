package com.gtdflow.widget.capture

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gtdflow.widget.ui.CaptureActivity

/**
 * Виджет «Захват» (Glance, 1×1): квадратная кнопка-ячейка с крупным «＋» в стиле остальных
 * виджетов (цвета/скругления — как у кнопки «＋» во «Входящих»). Тап открывает тот же
 * оверлей быстрого захвата [CaptureActivity] с поднятой клавиатурой. Пространство назначения
 * — из per-widget конфига (Glance-состояние, дефолт «Все»); данные vault виджету не нужны,
 * пересчёт для него не запускается.
 */
class CaptureWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                CaptureButton()
            }
        }
    }
}

@Composable
private fun CaptureButton() {
    val prefs = currentState<Preferences>()
    val namespace = CaptureWidgetState.namespaceOf(prefs)
    val intent = CaptureActivity.intent(LocalContext.current, namespace)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "＋",
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onPrimaryContainer,
            ),
        )
    }
}
