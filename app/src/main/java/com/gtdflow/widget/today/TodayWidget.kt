package com.gtdflow.widget.today

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gtdflow.widget.data.AppStore
import com.gtdflow.widget.engine.DeepLink
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodayItem
import com.gtdflow.widget.engine.TodaySection
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.ui.MainActivity
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.WidgetVaultGate
import com.gtdflow.widget.work.RefreshScheduler
import com.gtdflow.widget.work.RefreshWidgetsAction

/**
 * Виджет «Сегодня» (Glance): вертикальная лента дня из кэша AppStore (секция today,
 * не зависит от пространства — агрегат всех). Пустой день — «Свободно 🎉». Тап по
 * элементу — deep-link obsidian://open; тап по заголовку — обновление. Если кэша нет,
 * а vault выбран — ставим фоновое обновление и показываем «Загрузка…».
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val vault = AppStore.vaultConfig(context)
        val treeUri = vault.treeUri?.let(Uri::parse)
        val hasAccess = treeUri != null && VaultManager.hasAccess(context, treeUri)
        val gate = WidgetVaultGate.of(vault.isConfigured, hasAccess)
        val cache = AppStore.todayCache(context)
        // Планируем пересчёт только при реальном доступе — иначе refresh всё равно
        // сделает ранний return и «Загрузка…» зависнет.
        if (gate == WidgetVaultGate.READY && cache.todayJson == null) {
            RefreshScheduler.refreshNow(context)
        }
        val section = cache.todayJson?.let {
            runCatching { WidgetJson.decodeFromString(TodaySection.serializer(), it) }.getOrNull()
        }
        // Реальное локальное время показа (не generatedAt пересчёта) — для маркера «сейчас».
        val nowMinutes = TimeUtil.nowMinutes()
        provideContent {
            GlanceTheme {
                TodayContent(
                    gate = gate,
                    vaultName = vault.vaultName,
                    section = section,
                    updatedHhmm = cache.updatedHhmm,
                    nowMinutes = nowMinutes,
                )
            }
        }
    }
}

@Composable
private fun TodayContent(
    gate: WidgetVaultGate,
    vaultName: String?,
    section: TodaySection?,
    updatedHhmm: String?,
    nowMinutes: Int,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
    ) {
        Header(section, updatedHhmm)
        Spacer(GlanceModifier.height(6.dp))
        when (gate) {
            WidgetVaultGate.SELECT_VAULT ->
                CenterNote("Откройте приложение и выберите vault", openAppAction())
            WidgetVaultGate.ACCESS_LOST ->
                CenterNote("Доступ к vault потерян — откройте приложение и выберите папку", openAppAction())
            WidgetVaultGate.READY -> when {
                section == null -> CenterNote("Загрузка…")
                section.items.isEmpty() -> CenterNote("Свободно 🎉")
                else -> TodayList(section, vaultName, nowMinutes)
            }
        }
    }
}

/** Клик по заметке о vault открывает MainActivity (там выбор/переустановка папки). */
@Composable
private fun openAppAction(): Action =
    actionStartActivity(
        Intent(LocalContext.current, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

@Composable
private fun Header(section: TodaySection?, updatedHhmm: String?) {
    val date = section?.date ?: "Сегодня"
    val updated = updatedHhmm ?: "—"
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionRunCallback<RefreshWidgetsAction>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Сегодня, $date",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GlanceTheme.colors.onSurface,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = "обновлено $updated ⟳",
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun TodayList(section: TodaySection, vaultName: String?, nowMinutes: Int) {
    val nowHour = TimeUtil.hourOf(nowMinutes)
    val nowIndex = TodayNowLine.position(section.items, nowMinutes)
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        section.items.forEachIndexed { idx, entry ->
            if (idx == nowIndex) item(itemId = NOW_LINE_ID) { NowLine(nowMinutes) }
            item(itemId = entry.file.hashCode() * 31L + entry.line) {
                TodayCard(entry, isCurrentHour(entry, nowHour), vaultName)
            }
        }
        // Маркер «сейчас» ниже всех элементов (текущее время позже последнего начала).
        if (nowIndex == section.items.size) item(itemId = NOW_LINE_ID) { NowLine(nowMinutes) }
    }
}

/** Отдельная строка-маркер текущего времени: не зависит от наличия элемента в этом часе. */
@Composable
private fun NowLine(nowMinutes: Int) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "● ${TimeUtil.minutesToHhmm(nowMinutes)}",
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.primary,
            ),
        )
        Spacer(GlanceModifier.width(6.dp))
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(2.dp)
                .cornerRadius(1.dp)
                .background(GlanceTheme.colors.primary),
        ) {}
    }
}

@Composable
private fun TodayCard(item: TodayItem, currentHour: Boolean, vaultName: String?) {
    val bg: ColorProvider =
        if (currentHour) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant
    val open = if (vaultName != null) {
        actionStartActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(DeepLink.open(vaultName, item.file)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } else {
        actionRunCallback<RefreshWidgetsAction>()
    }
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(bg)
                .cornerRadius(10.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(open),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.width(58.dp)) {
                Text(
                    text = timeLabel(item),
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
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterNote(text: String, onClick: Action? = null) {
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

private fun timeLabel(item: TodayItem): String =
    if (item.allDay || item.startMinutes == null) "весь\nдень"
    else TimeUtil.formatRange(item.startMinutes, item.endMinutes)

private fun isCurrentHour(item: TodayItem, nowHour: Int): Boolean {
    val start = item.startMinutes ?: return false
    return TimeUtil.hourOf(start) == nowHour
}

/** itemId маркера «сейчас» — вне диапазона id элементов (file.hashCode()*31+line). */
private const val NOW_LINE_ID = Long.MIN_VALUE
