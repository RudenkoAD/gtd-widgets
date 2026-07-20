package com.gtdflow.widget.inbox

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gtdflow.widget.data.AppStore
import com.gtdflow.widget.engine.InboxItem
import com.gtdflow.widget.engine.InboxSection
import com.gtdflow.widget.engine.WidgetErrorText
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.ui.CaptureActivity
import com.gtdflow.widget.ui.InboxTaskSheetActivity
import com.gtdflow.widget.ui.MainActivity
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.WidgetVaultGate
import com.gtdflow.widget.work.RefreshScheduler
import com.gtdflow.widget.work.RefreshWidgetsAction

/**
 * Виджет «Входящие» (Glance): заголовок «Входящие · <пространство>», список задач с
 * чекбоксом слева (тап отмечает `- [x]` в файле) и кнопкой «+» (быстрый захват).
 * Пространство — из per-widget конфига (Glance-состояние), задаётся конфигуратором.
 * Данные — из кэша этого виджета (секция inbox выбранного пространства).
 */
class InboxWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val vault = AppStore.vaultConfig(context)
        val treeUri = vault.treeUri?.let(Uri::parse)
        val hasAccess = treeUri != null && VaultManager.hasAccess(context, treeUri)
        val gate = WidgetVaultGate.of(vault.isConfigured, hasAccess)
        val prefs: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        // Планируем пересчёт только при реальном доступе И только пока нет ни данных,
        // ни зафиксированной ошибки — иначе refresh либо рано выйдет («Загрузка…»
        // зависнет), либо будет молотить впустую при устойчивой ошибке (её показываем).
        if (gate == WidgetVaultGate.READY &&
            prefs[InboxWidgetState.INBOX_JSON] == null &&
            prefs[InboxWidgetState.ERROR] == null
        ) {
            RefreshScheduler.refreshNow(context)
        }
        val vaultName = vault.vaultName
        provideContent {
            GlanceTheme {
                InboxContent(gate = gate, vaultName = vaultName)
            }
        }
    }
}

@Composable
private fun InboxContent(gate: WidgetVaultGate, vaultName: String?) {
    val prefs = currentState<Preferences>()
    val namespace = InboxWidgetState.namespaceOf(prefs)
    val updated = prefs[InboxWidgetState.UPDATED]
    val error = prefs[InboxWidgetState.ERROR]
    val notice = prefs[InboxWidgetState.NOTICE]
    val section = prefs[InboxWidgetState.INBOX_JSON]?.let {
        runCatching { WidgetJson.decodeFromString(InboxSection.serializer(), it) }.getOrNull()
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
    ) {
        Header(namespace, updated, stale = section != null && error != null)
        Spacer(GlanceModifier.height(6.dp))
        // Транзиентная заметка промаха чекбокса («Файл изменился — обновите виджет»):
        // строкой НАД списком, не пряча данные; успешный пересчёт её снимет.
        if (notice != null) {
            NoticeLine(notice)
        }
        when (gate) {
            WidgetVaultGate.SELECT_VAULT ->
                Note("Откройте приложение и выберите vault", openAppAction())
            WidgetVaultGate.ACCESS_LOST ->
                Note("Доступ к vault потерян — откройте приложение и выберите папку", openAppAction())
            WidgetVaultGate.READY -> when {
                section == null && error != null -> Note("Ошибка: $error", openAppAction())
                section == null -> Note("Загрузка…")
                section.items.isEmpty() -> Note("Входящих нет")
                else -> InboxList(section, namespace, vaultName)
            }
        }
    }
}

/** Клик по заметке о vault открывает MainActivity (выбор/переустановка папки). */
@Composable
private fun openAppAction(): Action =
    actionStartActivity(
        Intent(LocalContext.current, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

@Composable
private fun Header(namespace: String, updated: String?, stale: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionRunCallback<RefreshWidgetsAction>()),
        ) {
            Text(
                text = "Входящие · $namespace",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
                maxLines = 1,
            )
            if (updated != null) {
                Text(
                    // stale: показан кэш, но последний пересчёт упал — честное « · ошибка».
                    text = "обновлено ${WidgetErrorText.updatedLabel(updated, stale)}",
                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        }
        AddButton(namespace)
    }
}

@Composable
private fun AddButton(namespace: String) {
    val intent = CaptureActivity.intent(androidx.glance.LocalContext.current, namespace)
    Box(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(18.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "＋",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onPrimaryContainer,
            ),
        )
    }
}

@Composable
private fun InboxList(section: InboxSection, namespace: String, vaultName: String?) {
    val aggregate = InboxWidgetState.isAggregate(namespace)
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(section.items, itemId = { it.file.hashCode() * 31L + it.line }) { item ->
            InboxRow(item, aggregate, vaultName)
        }
    }
}

@Composable
private fun InboxRow(item: InboxItem, aggregate: Boolean, vaultName: String?) {
    val params: ActionParameters = actionParametersOf(
        InboxToggleAction.KEY_FILE to item.file,
        InboxToggleAction.KEY_LINE to item.line,
        InboxToggleAction.KEY_TITLE to item.title,
    )
    // Тап по тексту задачи открывает малый оверлей правки (текст/место/выполнено).
    val openEdit = actionStartActivity(
        InboxTaskSheetActivity.intent(LocalContext.current, item, vaultName),
    )
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Чекбокс — ЧИСТО ВИЗУАЛЬНЫЙ глиф (входящие всегда не отмечены: отмеченная строка
        // уходит из списка). Тап-зона — охватывающий Box с обычным clickable/actionRunCallback,
        // НЕ compound-button onCheckedChange: у Glance CheckBox своя рекомпозиция из состояния
        // ДО записи перекрывала оптимистичный updateAll, и строка «висла» до полного пересчёта
        // (~1.9 c). Обычный clickable отрисовывает оптимистичный патч ДО пересчёта — тем же
        // путём (и той же ценой updateAll), что и оптимистичный захват «+».
        Box(
            modifier = GlanceModifier
                .padding(start = 2.dp, end = 8.dp)
                .clickable(actionRunCallback<InboxToggleAction>(params)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "☐",
                style = TextStyle(fontSize = 20.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        Column(
            modifier = GlanceModifier.defaultWeight().padding(start = 4.dp).clickable(openEdit),
        ) {
            Text(
                text = item.title,
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
        // Агрегат «Все»: метка пространства справа серым.
        if (aggregate && item.namespace.isNotBlank()) {
            Text(
                text = item.namespace,
                style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1,
            )
        }
    }
}

/** Строка транзиентной заметки (промах чекбокса) над списком: заметно, но без модальности. */
@Composable
private fun NoticeLine(text: String) {
    Text(
        text = "⚠ $text",
        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
        style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.error),
        maxLines = 2,
    )
}

@Composable
private fun Note(text: String, onClick: Action? = null) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .let { if (onClick != null) it.clickable(onClick) else it },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}
