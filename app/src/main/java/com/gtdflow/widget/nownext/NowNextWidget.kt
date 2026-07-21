package com.gtdflow.widget.nownext

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
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gtdflow.widget.R
import com.gtdflow.widget.data.AppStore
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodayItem
import com.gtdflow.widget.engine.TodaySection
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.ui.EventSheetActivity
import com.gtdflow.widget.ui.MainActivity
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.WidgetVaultGate
import com.gtdflow.widget.work.RefreshScheduler

/**
 * Виджет «Сейчас → Далее» (Glance, 4×1, сжимаем до 3×1): один взгляд — что идёт сейчас и
 * что следующее СЕГОДНЯ. Данные — тот же кэш AppStore.todayCache, что у «Сегодня» (агрегат
 * всех пространств); выбор {current, next} — чистая [NowNextLogic], текст строк — [NowNextText].
 * Только абсолютное время (у идущего с концом «до HH:mm», иначе старт «HH:mm»), без «осталось
 * N минут» — виджет не протухает между обновлениями, а точные переходы на границах доводит
 * аларм (см. [NowNextAlarms]). При 3×1 место 📍 прячется. Тап по строке — шторка деталей
 * [EventSheetActivity]; тап по пустому — приложение.
 */
class NowNextWidget : GlanceAppWidget() {

    // Exact — читаем реальный размер (LocalSize) на каждую перерисовку, чтобы на 3×1 спрятать
    // место 📍 (двухстрочный лимит), а на 4×1 показать.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val vault = AppStore.vaultConfig(context)
        val treeUri = vault.treeUri?.let(Uri::parse)
        val hasAccess = treeUri != null && VaultManager.hasAccess(context, treeUri)
        val gate = WidgetVaultGate.of(vault.isConfigured, hasAccess)
        val cache = AppStore.todayCache(context)
        // Кэша нет и ошибок нет, но доступ есть — ставим фоновый пересчёт (как «Сегодня»).
        if (gate == WidgetVaultGate.READY && cache.todayJson == null && cache.error == null) {
            RefreshScheduler.refreshNow(context)
        }
        val section = cache.todayJson?.let {
            runCatching { WidgetJson.decodeFromString(TodaySection.serializer(), it) }.getOrNull()
        }
        // Реальное локальное время показа (не generatedAt пересчёта) — для выбора «сейчас».
        val nowMinutes = TimeUtil.nowMinutes()
        provideContent {
            GlanceTheme {
                NowNextContent(gate, vault.vaultName, section, cache.error, nowMinutes)
            }
        }
    }
}

@Composable
private fun NowNextContent(
    gate: WidgetVaultGate,
    vaultName: String?,
    section: TodaySection?,
    error: String?,
    nowMinutes: Int,
) {
    // 4 ячейки ≈ 250dp, 3 ячейки ≈ 180dp — порог 220dp разделяет «широкий»/«узкий».
    val wide = LocalSize.current.width >= 220.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        when (gate) {
            WidgetVaultGate.SELECT_VAULT -> CenterNote("Откройте приложение и выберите vault", openApp())
            WidgetVaultGate.ACCESS_LOST -> CenterNote("Доступ к vault потерян — откройте приложение", openApp())
            WidgetVaultGate.READY -> when {
                section == null && error != null -> CenterNote("Ошибка: $error", openApp())
                section == null -> CenterNote("Загрузка…")
                // Кэш показан, но последний пересчёт упал (error != null) — помечаем несвежесть.
                else -> NowNextBody(section, vaultName, nowMinutes, wide, stale = error != null)
            }
        }
    }
}

@Composable
private fun NowNextBody(
    section: TodaySection,
    vaultName: String?,
    nowMinutes: Int,
    wide: Boolean,
    stale: Boolean,
) {
    val nn = NowNextLogic.select(section.items, nowMinutes)
    val current = nn.current
    val next = nn.next
    // Пометку несвежести (stale) вешаем только на ПОСЛЕДНЮЮ строку — не дублируем.
    // if/else-if даёт чистый smart-cast current/next к non-null в своих ветках.
    if (current != null) {
        // Идёт текущее (первая строка — без пометки, она не последняя).
        PrimaryLine(
            text = NowNextText.current(current, wide),
            onClick = openSheet(current, section.date, vaultName),
        )
        if (next != null) {
            SecondaryLine(
                text = NowNextText.withStale(NowNextText.next(next, prefix = "→ "), stale),
                onClick = openSheet(next, section.date, vaultName),
            )
        } else {
            // Идёт текущее, но дальше сегодня ничего — та же формулировка, что у ALL_PAST.
            val nothingMore = LocalContext.current.getString(R.string.nownext_empty_all_past)
            SecondaryLine(text = NowNextText.withStale(nothingMore, stale), onClick = openApp())
        }
    } else if (next != null) {
        // Текущего нет, но есть предстоящее — единственная строка, к ней и пометка.
        PrimaryLine(
            text = NowNextText.withStale(
                NowNextText.next(next, prefix = "Далее: ", withLocation = wide),
                stale,
            ),
            onClick = openSheet(next, section.date, vaultName),
        )
    } else {
        // Ни current, ни next: честно объясняем почему (см. [NowNextLogic.emptyReason]) —
        // пустой день ≠ «только всё-день/недатированные» ≠ «всё со временем прошло».
        CenterNote(NowNextText.withStale(emptyText(NowNextLogic.emptyReason(section.items)), stale), openApp())
    }
}

/** Текст честного пустого состояния «Сейчас» по причине (строки — из strings.xml). */
@Composable
private fun emptyText(reason: NowNextLogic.EmptyReason): String =
    LocalContext.current.getString(
        when (reason) {
            NowNextLogic.EmptyReason.NO_ITEMS -> R.string.nownext_empty_no_items
            NowNextLogic.EmptyReason.NO_TIMED -> R.string.nownext_empty_no_timed
            NowNextLogic.EmptyReason.ALL_PAST -> R.string.nownext_empty_all_past
        },
    )

/** Первая строка (крупнее): «Сейчас: …» / «Далее: …». Одна строка, ellipsize. Кликается
 *  container-Row (как в остальных виджетах), не сам Text. */
@Composable
private fun PrimaryLine(text: String, onClick: Action) {
    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick)) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface,
            ),
        )
    }
}

/** Вторая строка: «→ HH:mm …» / «Сегодня больше ничего». Одна строка, ellipsize. */
@Composable
private fun SecondaryLine(text: String, onClick: Action) {
    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick)) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

/** Шторка деталей элемента (как из «Сегодня»/«Агенды»). */
@Composable
private fun openSheet(item: TodayItem, dayIso: String, vaultName: String?): Action =
    actionStartActivity(EventSheetActivity.intent(LocalContext.current, item, dayIso, vaultName))

/** Клик по пустому/служебному состоянию открывает приложение. */
@Composable
private fun openApp(): Action =
    actionStartActivity(
        Intent(LocalContext.current, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

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
            maxLines = 2,
            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}
