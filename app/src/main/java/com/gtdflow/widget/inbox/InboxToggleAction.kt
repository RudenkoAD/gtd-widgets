package com.gtdflow.widget.inbox

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultWriter
import com.gtdflow.widget.work.RefreshScheduler

/**
 * Отметка входящей задачи выполненной по тапу чекбокса.
 *
 * Перечитывает актуальный файл и ищет строку (по номеру, затем по тексту — см.
 * TaskLineToggle), пишет `- [x]`. При промахе (строка сместилась/исчезла) молча
 * запускает рефреш — виджет пересоберётся из свежего состояния vault. После записи
 * тоже рефреш: отмеченная задача выпадает из inbox-запроса и исчезает из списка.
 */
class InboxToggleAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val file = parameters[KEY_FILE]
        val line = parameters[KEY_LINE]
        val title = parameters[KEY_TITLE]
        if (file != null && line != null && title != null) {
            val treeUri = VaultManager.treeUri(context)
            if (treeUri != null && VaultManager.hasAccess(context, treeUri)) {
                runCatching {
                    VaultWriter.markInboxDone(context, treeUri, file, line, title)
                }
            }
        }
        // и при успехе, и при промахе — пересобрать виджеты из актуального vault
        RefreshScheduler.refreshNow(context)
    }

    companion object {
        val KEY_FILE = ActionParameters.Key<String>("file")
        val KEY_LINE = ActionParameters.Key<Int>("line")
        val KEY_TITLE = ActionParameters.Key<String>("title")
    }
}
