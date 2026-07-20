package com.gtdflow.widget.agenda

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.gtdflow.widget.work.RefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Конфигуратор виджета «Агенда»: выбор числа дней (7/14/30). По выбору пишем число в
 * per-widget Glance-состояние, обновляем виджет и возвращаем RESULT_OK с id виджета
 * (иначе система отменит добавление). По образцу InboxConfigActivity.
 */
class AgendaConfigActivity : ComponentActivity() {

    private val scope: CoroutineScope = MainScope()
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigScreen(onPick = ::choose)
                }
            }
        }
    }

    private fun choose(days: Int) {
        scope.launch {
            val glanceId = GlanceAppWidgetManager(this@AgendaConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@AgendaConfigActivity, glanceId) { prefs ->
                prefs[AgendaWidgetState.DAYS] = days
            }
            AgendaWidget().update(this@AgendaConfigActivity, glanceId)
            RefreshScheduler.refreshNow(this@AgendaConfigActivity)
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@Composable
private fun ConfigScreen(onPick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "Сколько дней показывать", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        for (days in AgendaWidgetState.CHOICES) {
            OutlinedButton(
                onClick = { onPick(days) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("$days ${dayWord(days)}") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Русское слово «дней/дня» для кнопки (7/14/30 → «дней», кроме форм на 2-4). */
private fun dayWord(days: Int): String {
    val mod100 = days % 100
    val mod10 = days % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}
