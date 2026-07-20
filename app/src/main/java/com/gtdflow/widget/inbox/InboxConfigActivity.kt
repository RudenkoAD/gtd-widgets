package com.gtdflow.widget.inbox

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.gtdflow.widget.data.NamespaceCatalog
import com.gtdflow.widget.work.RefreshScheduler
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * Конфигуратор виджета «Входящие» (плейсхолдер зоны окружения): выбор пространства.
 * Список = встроенные «Общее»/«Все» + пользовательские пространства (NamespaceCatalog).
 * По выбору пишем имя пространства в per-widget Glance-состояние, обновляем виджет и
 * возвращаем RESULT_OK с id виджета (иначе система отменит добавление виджета).
 */
class InboxConfigActivity : ComponentActivity() {

    private val scope: CoroutineScope = MainScope()
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // По умолчанию — отмена: если пользователь выйдет, виджет не добавится.
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

    private fun choose(namespace: String) {
        scope.launch {
            val glanceId = GlanceAppWidgetManager(this@InboxConfigActivity).getGlanceIdBy(appWidgetId)
            // Форма updateAppWidgetState(context, glanceId){...} даёт МУТАБЕЛЬНЫЕ prefs
            // (PreferencesGlanceStateDefinition по умолчанию) — как в WidgetService.
            updateAppWidgetState(this@InboxConfigActivity, glanceId) { prefs ->
                prefs[InboxWidgetState.NAMESPACE] = namespace
            }
            InboxWidget().update(this@InboxConfigActivity, glanceId)
            RefreshScheduler.refreshNow(this@InboxConfigActivity)
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@Composable
private fun ConfigScreen(onPick: (String) -> Unit) {
    val context = LocalContext.current
    var labels by remember { mutableStateOf(listOf(InboxWidgetState.DEFAULT_NAMESPACE, "Все")) }

    LaunchedEffect(Unit) {
        val user = runCatching { NamespaceCatalog.userNamespaces(context).map { it.name } }
            .getOrDefault(emptyList())
        labels = (listOf(InboxWidgetState.DEFAULT_NAMESPACE, "Все") + user).distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "Пространство виджета", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        for (label in labels) {
            OutlinedButton(
                onClick = { onPick(label) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(label) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
