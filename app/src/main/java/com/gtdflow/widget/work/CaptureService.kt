package com.gtdflow.widget.work

import android.content.Context
import android.net.Uri
import com.gtdflow.widget.engine.EngineRunner
import com.gtdflow.widget.engine.InboxItem
import com.gtdflow.widget.engine.QuickJsEngine
import com.gtdflow.widget.inbox.CaptureNamespace
import com.gtdflow.widget.inbox.InboxWidgetState
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultReader
import com.gtdflow.widget.vault.VaultWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Быстрый захват из виджета «Входящие»: строит строку-задачу и путь файла входящих
 * ЯДРОМ (buildCaptureLine/captureTargetPath), затем пишет в vault (создав файл с
 * gtd-inbox при необходимости) и запускает пересчёт виджетов.
 */
object CaptureService {

    sealed interface Outcome {
        data object Success : Outcome
        data class Failure(val message: String) : Outcome
    }

    suspend fun capture(context: Context, namespace: String, text: String, location: String?): Outcome {
        if (text.isBlank()) return Outcome.Failure("Пустой текст")

        val treeUri: Uri = VaultManager.treeUri(context)
            ?: return Outcome.Failure("Vault не выбран")
        if (!VaultManager.hasAccess(context, treeUri)) {
            return Outcome.Failure("Нет доступа к vault")
        }

        val dataJson = withContext(Dispatchers.IO) {
            VaultReader.readDataJsonOnly(context, treeUri)
        }

        val built = try {
            EngineRunner.use(context) { engine ->
                val line = engine.buildCaptureLine(text, location)
                // «Все»/«Общее» → null (пишем в «Общее»); иначе — конкретное пространство.
                val target = engine.captureTargetPath(dataJson, CaptureNamespace.forWrite(namespace))
                line to target
            }
        } catch (e: QuickJsEngine.EngineException) {
            return Outcome.Failure(e.message ?: "Ошибка ядра захвата")
        }

        val ok = withContext(Dispatchers.IO) {
            VaultWriter.capture(context, treeUri, built.second, built.first)
        }
        if (!ok) return Outcome.Failure("Не удалось записать во входящие")

        // Оптимистично показать новую задачу сразу (line=0 — плейсхолдер, пересчёт заменит
        // на честный элемент). Метка пространства: «Все»/«Общее» пишутся в «Общее».
        val displayNs = when (namespace) {
            InboxWidgetState.ALL_NAMESPACE, InboxWidgetState.DEFAULT_NAMESPACE ->
                InboxWidgetState.DEFAULT_NAMESPACE
            else -> namespace
        }
        val optimisticItem = InboxItem(
            title = text.trim(),
            file = built.second,
            line = 0,
            location = location?.takeIf { it.isNotBlank() },
            namespace = displayNs,
        )
        OptimisticInbox.addItem(context, optimisticItem, setOf(namespace, displayNs))

        RefreshScheduler.refreshNow(context)
        return Outcome.Success
    }
}
