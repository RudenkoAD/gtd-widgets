package com.gtdflow.widget.work

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.gtdflow.widget.engine.InboxItem
import com.gtdflow.widget.engine.InboxSection
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.inbox.InboxOptimistic
import com.gtdflow.widget.inbox.InboxWidget
import com.gtdflow.widget.inbox.InboxWidgetState
import com.gtdflow.widget.perf.Perf

/**
 * Мгновенный (оптимистичный) патч кэша виджетов «Входящие» БЕЗ обхода vault: правит
 * закэшированную секцию в Glance-состоянии каждого экземпляра и дёргает updateAll.
 * Даёт визуальный отклик за десятки мс; честный пересчёт (WidgetService.refresh…) идёт
 * следом и перезаписывает секцию. Логика патчей — чистый [InboxOptimistic].
 */
object OptimisticInbox {

    private val ser = InboxSection.serializer()

    /** Убрать строку файл+номер из всех экземпляров и перерисовать (чекбокс/«Выполнено»). */
    suspend fun removeLine(context: Context, file: String, line: Int) {
        Perf.span("toggle.optimistic") {
            patch(context) { section, _ -> InboxOptimistic.removeLine(section, file, line) }
        }
    }

    /** Обновить текст/место строки файл+номер во всех экземплярах (правка из шторки). */
    suspend fun editLine(context: Context, file: String, line: Int, title: String, location: String?) {
        Perf.span("edit.optimistic") {
            patch(context) { section, _ -> InboxOptimistic.editLine(section, file, line, title, location) }
        }
    }

    /**
     * Добавить элемент в те экземпляры, что его показали бы: агрегат «Все» — всегда,
     * иначе только совпадающие с [displayNamespaces] (пространство, откуда шёл захват).
     */
    suspend fun addItem(context: Context, item: InboxItem, displayNamespaces: Set<String>) {
        Perf.span("capture.optimistic") {
            patch(context) { section, ns ->
                if (InboxWidgetState.isAggregate(ns) || ns in displayNamespaces) {
                    InboxOptimistic.prepend(section, item)
                } else {
                    null
                }
            }
        }
    }

    /**
     * Прогнать [transform] по секции каждого виджета «Входящие»; null или совпадение —
     * пропустить (не писать). После всех правок — единый updateAll.
     */
    private suspend fun patch(
        context: Context,
        transform: (section: InboxSection, namespace: String) -> InboxSection?,
    ) {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(InboxWidget::class.java)
        var changed = false
        for (id in ids) {
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            val json = prefs[InboxWidgetState.INBOX_JSON] ?: continue
            val section = runCatching { WidgetJson.decodeFromString(ser, json) }.getOrNull() ?: continue
            val ns = InboxWidgetState.namespaceOf(prefs)
            val next = transform(section, ns) ?: continue
            if (next != section) {
                updateAppWidgetState(context, id) { mutablePrefs ->
                    mutablePrefs[InboxWidgetState.INBOX_JSON] = WidgetJson.encodeToString(ser, next)
                }
                changed = true
            }
        }
        if (changed) InboxWidget().updateAll(context)
    }
}
