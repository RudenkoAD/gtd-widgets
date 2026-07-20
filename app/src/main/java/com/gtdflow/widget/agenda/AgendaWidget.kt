package com.gtdflow.widget.agenda

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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
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
import com.gtdflow.widget.engine.AgendaSection
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.WidgetErrorText
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.today.FeedItemCard
import com.gtdflow.widget.ui.MainActivity
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.WidgetVaultGate
import com.gtdflow.widget.work.RefreshScheduler
import com.gtdflow.widget.work.RefreshWidgetsAction

/**
 * Виджет «Агенда» (Glance): скролл ближайших дней (число — из per-widget конфига,
 * дефолт 7). Заголовок дня («Пн, 21 июля», локаль ru — [AgendaDayHeader]), под ним
 * элементы (время одной строкой + название + 📍). Пустые дни пропускаются. Шапка как у
 * «Сегодня» (обновлено HH:mm, тап-refresh). Тап по элементу — та же шторка деталей.
 * Данные — из per-widget Glance-состояния (секция agenda под нужное число дней).
 */
class AgendaWidget : GlanceAppWidget() {

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
            prefs[AgendaWidgetState.AGENDA_JSON] == null &&
            prefs[AgendaWidgetState.ERROR] == null
        ) {
            RefreshScheduler.refreshNow(context)
        }
        val vaultName = vault.vaultName
        provideContent {
            GlanceTheme {
                AgendaContent(gate = gate, vaultName = vaultName)
            }
        }
    }
}

@Composable
private fun AgendaContent(gate: WidgetVaultGate, vaultName: String?) {
    val prefs = currentState<Preferences>()
    val updated = prefs[AgendaWidgetState.UPDATED]
    val error = prefs[AgendaWidgetState.ERROR]
    val section = prefs[AgendaWidgetState.AGENDA_JSON]?.let {
        runCatching { WidgetJson.decodeFromString(AgendaSection.serializer(), it) }.getOrNull()
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
    ) {
        Header(updated, stale = section != null && error != null)
        Spacer(GlanceModifier.height(6.dp))
        when (gate) {
            WidgetVaultGate.SELECT_VAULT ->
                Note("Откройте приложение и выберите vault", openAppAction())
            WidgetVaultGate.ACCESS_LOST ->
                Note("Доступ к vault потерян — откройте приложение и выберите папку", openAppAction())
            WidgetVaultGate.READY -> {
                val days = section?.days?.filter { it.items.isNotEmpty() } ?: emptyList()
                when {
                    section == null && error != null -> Note("Ошибка: $error", openAppAction())
                    section == null -> Note("Загрузка…")
                    days.isEmpty() -> Note("Ближайшие дни свободны 🎉")
                    else -> AgendaList(section.days, vaultName)
                }
            }
        }
    }
}

@Composable
private fun openAppAction(): Action =
    actionStartActivity(
        Intent(LocalContext.current, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

@Composable
private fun Header(updated: String?, stale: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionRunCallback<RefreshWidgetsAction>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Агенда",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GlanceTheme.colors.onSurface,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            // stale: показан кэш, но последний пересчёт упал — честное « · ошибка».
            text = "обновлено ${WidgetErrorText.updatedLabel(updated, stale)} ⟳",
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun AgendaList(days: List<com.gtdflow.widget.engine.AgendaDay>, vaultName: String?) {
    val todayIso = TimeUtil.todayIso()
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        for (day in days) {
            if (day.items.isEmpty()) continue // пустые дни пропускаем
            item(itemId = day.date.hashCode().toLong()) { DayHeader(day.date, todayIso) }
            for (entry in day.items) {
                item(itemId = day.date.hashCode() * 131L + entry.file.hashCode() * 31L + entry.line) {
                    FeedItemCard(entry, day.date, vaultName, highlight = false)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(dateIso: String, todayIso: String) {
    Text(
        text = AgendaDayHeader.format(dateIso, todayIso),
        modifier = GlanceModifier.padding(top = 4.dp, bottom = 4.dp),
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = GlanceTheme.colors.primary,
        ),
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
