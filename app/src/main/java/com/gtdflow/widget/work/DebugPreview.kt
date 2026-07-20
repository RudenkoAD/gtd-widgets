package com.gtdflow.widget.work

import android.content.Context
import android.net.Uri
import com.gtdflow.widget.engine.EngineRunner
import com.gtdflow.widget.engine.InboxItem
import com.gtdflow.widget.engine.QuickJsEngine
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodayItem
import com.gtdflow.widget.engine.WidgetData
import com.gtdflow.widget.engine.WidgetInput
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Текстовый дамп данных виджета для отладки на экране MainActivity.
 *
 * Читает vault, гоняет ядро в QuickJS (агрегат «сегодня» + входящие «Общего») и
 * форматирует человекочитаемый снимок: сколько файлов ушло в движок, лента дня,
 * входящие, список пространств и накопленные ошибки. Ничего не пишет и не кэширует —
 * это диагностика по запросу («Обновить превью»), не путь виджетов.
 */
object DebugPreview {

    /** Итог построения: либо готовый текст, либо причина, почему превью недоступно. */
    sealed interface Result {
        data class Ready(val text: String) : Result
        data class Unavailable(val reason: String) : Result
    }

    suspend fun build(context: Context): Result {
        val treeUri: Uri = VaultManager.treeUri(context)
            ?: return Result.Unavailable("Vault не выбран — нажмите «Выбрать vault».")
        if (!VaultManager.hasAccess(context, treeUri)) {
            return Result.Unavailable("Доступ к vault отозван — выберите папку заново.")
        }

        val snapshot = withContext(Dispatchers.IO) { VaultReader.read(context, treeUri) }
        val todayIso = TimeUtil.todayIso()
        val nowMinutes = TimeUtil.nowMinutes()

        val data: WidgetData = try {
            EngineRunner.use(context) { engine ->
                engine.compute(
                    WidgetInput(snapshot.files, snapshot.dataJson, todayIso, nowMinutes, null),
                )
            }
        } catch (e: QuickJsEngine.EngineException) {
            return Result.Unavailable("Ошибка движка: ${e.message}")
        }

        return Result.Ready(format(data, snapshot.files.size, snapshot.dataJson != null))
    }

    private fun format(data: WidgetData, fileCount: Int, hasDataJson: Boolean): String {
        val sb = StringBuilder()
        sb.append("Файлов .md в движок: ").append(fileCount).append('\n')
        sb.append("data.json плагина: ").append(if (hasDataJson) "найден" else "нет (дефолты)").append('\n')
        sb.append('\n')

        sb.append("СЕГОДНЯ · ").append(data.today.date)
            .append(" (сформировано ").append(shortTime(data.today.generatedAt)).append(")\n")
        if (data.today.items.isEmpty()) {
            sb.append("  — свободно 🎉\n")
        } else {
            for (item in data.today.items) sb.append(todayLine(item)).append('\n')
        }
        sb.append('\n')

        sb.append("ВХОДЯЩИЕ · ").append(data.inbox.namespace)
            .append(" (").append(data.inbox.items.size).append(")\n")
        if (data.inbox.items.isEmpty()) {
            sb.append("  — пусто\n")
        } else {
            for (item in data.inbox.items) sb.append(inboxLine(item)).append('\n')
        }
        sb.append('\n')

        sb.append("ПРОСТРАНСТВА: ")
        sb.append(if (data.namespaces.isEmpty()) "— (только «Общее»/«Все»)" else data.namespaces.joinToString(", ") { it.name })
        sb.append('\n')

        if (data.errors.isNotEmpty()) {
            sb.append('\n').append("ОШИБКИ (").append(data.errors.size).append("):\n")
            for (err in data.errors) sb.append("  • ").append(err).append('\n')
        }
        return sb.toString().trimEnd('\n')
    }

    private fun todayLine(item: TodayItem): String {
        val time = if (item.allDay || item.startMinutes == null) {
            "весь день"
        } else {
            TimeUtil.formatRange(item.startMinutes, item.endMinutes)
        }
        val bullet = if (item.isEvent) "• " else "☐ "
        val loc = if (!item.location.isNullOrBlank()) "  📍 ${item.location}" else ""
        return "  %-11s %s%s%s".format(time, bullet, item.title, loc)
    }

    private fun inboxLine(item: InboxItem): String {
        val loc = if (!item.location.isNullOrBlank()) "  📍 ${item.location}" else ""
        return "  ☐ ${item.title}$loc"
    }

    /** 'YYYY-MM-DDTHH:mm' → 'HH:mm' (для строки «сформировано»). */
    private fun shortTime(generatedAt: String): String =
        generatedAt.substringAfter('T', generatedAt)
}
