package com.gtdflow.widget.work

import android.content.Context
import android.net.Uri
import com.gtdflow.widget.engine.EditErrorText
import com.gtdflow.widget.engine.EditLineReply
import com.gtdflow.widget.engine.EngineRunner
import com.gtdflow.widget.engine.QuickJsEngine
import com.gtdflow.widget.vault.TaskLineEdit
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Сохранение правки одной строки задачи/события из шторки деталей и оверлея входящих.
 *
 * Поток: перечитать актуальный файл (не кэш), найти целевую строку, построить
 * отредактированную строку ЯДРОМ (buildEditedLine), записать по точному якорю и
 * запустить пересчёт виджетов. Любая рассинхронизация (файл изменился синком) —
 * НЕ пишем, возвращаем понятную ошибку. Порядок IO/движок такой же, как у
 * CaptureService: чтение/запись — на Dispatchers.IO, вызовы движка — на выделенном
 * однопоточном диспетчере внутри EngineRunner.use.
 */
object EditService {

    const val FILE_CHANGED = "Файл изменился — обновите виджет"

    sealed interface Outcome {
        data object Success : Outcome
        data class Failure(val message: String) : Outcome
    }

    /**
     * Правка элемента ленты «Сегодня»/«Агенды»: цель — строка [oneBasedLine], ТОЧНО
     * равная [rawLine] (rawLine из ядра — источник правки и якорь). [editsJson] —
     * см. LineEdits.toJson.
     */
    suspend fun saveTodayEdit(
        context: Context,
        filePath: String,
        oneBasedLine: Int,
        rawLine: String,
        editsJson: String,
    ): Outcome = save(context, filePath, editsJson) { content ->
        val idx = TaskLineEdit.anchorExact(content, oneBasedLine, rawLine) ?: return@save null
        idx to rawLine
    }

    /**
     * Правка задачи из «Входящих»: ядро не отдаёт rawLine, поэтому находим незакрытый
     * чекбокс по номеру+заголовку, а его сырой текст берём из файла как аргумент
     * buildEditedLine. [title] — Task.description из элемента входящих.
     */
    suspend fun saveInboxEdit(
        context: Context,
        filePath: String,
        oneBasedLine: Int,
        title: String,
        editsJson: String,
        newTitle: String = title,
        newLocation: String? = null,
    ): Outcome = save(
        context,
        filePath,
        editsJson,
        // Оптимистично поправить текст/место строки во «Входящих» до пересчёта.
        afterWrite = { OptimisticInbox.editLine(context, filePath, oneBasedLine, newTitle, newLocation) },
    ) { content ->
        val located = TaskLineEdit.locate(content, oneBasedLine, title) ?: return@save null
        located.index to located.rawLine
    }

    /**
     * Отметить задачу «Входящих» выполненной из оверлея (кнопка «Выполнено»). Тот же
     * путь, что у чекбокса виджета (VaultWriter.markInboxDone: перечитать → `- [x]` по
     * номеру+тексту → записать), но из Activity. Промах (строка сместилась) — Failure.
     */
    suspend fun markInboxDone(
        context: Context,
        filePath: String,
        oneBasedLine: Int,
        title: String,
    ): Outcome {
        val treeUri: Uri = VaultManager.treeUri(context)
            ?: return Outcome.Failure("Vault не выбран")
        if (!VaultManager.hasAccess(context, treeUri)) {
            return Outcome.Failure("Нет доступа к vault")
        }
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                VaultWriter.markInboxDone(context, treeUri, filePath, oneBasedLine, title)
            }.getOrDefault(false)
        }
        if (!ok) return Outcome.Failure(FILE_CHANGED)
        // Оптимистично убрать задачу из виджетов до пересчёта (как чекбокс).
        OptimisticInbox.removeLine(context, filePath, oneBasedLine)
        RefreshScheduler.refreshNow(context)
        return Outcome.Success
    }

    /**
     * Общий скелет: доступ → чтение файла → поиск цели ([target] даёт индекс+rawLine
     * или null=«файл изменился») → buildEditedLine → запись по индексу → рефреш.
     */
    private suspend fun save(
        context: Context,
        filePath: String,
        editsJson: String,
        afterWrite: (suspend () -> Unit)? = null,
        target: (content: String) -> Pair<Int, String>?,
    ): Outcome {
        val treeUri: Uri = VaultManager.treeUri(context)
            ?: return Outcome.Failure("Vault не выбран")
        if (!VaultManager.hasAccess(context, treeUri)) {
            return Outcome.Failure("Нет доступа к vault")
        }

        val content = withContext(Dispatchers.IO) {
            VaultWriter.readFile(context, treeUri, filePath)
        } ?: return Outcome.Failure(FILE_CHANGED)

        val (index, rawLine) = target(content) ?: return Outcome.Failure(FILE_CHANGED)

        val reply = try {
            EngineRunner.use(context) { engine ->
                EditLineReply.parse(engine.buildEditedLine(rawLine, editsJson))
            }
        } catch (e: QuickJsEngine.EngineException) {
            return Outcome.Failure(e.message ?: "Ошибка ядра правки")
        }
        if (!reply.ok || reply.line == null) {
            return Outcome.Failure(EditErrorText.humanize(reply.error))
        }

        val next = TaskLineEdit.replaceAt(content, index, reply.line)
        val ok = withContext(Dispatchers.IO) {
            VaultWriter.writeFile(context, treeUri, filePath, next)
        }
        if (!ok) return Outcome.Failure("Не удалось записать изменение")

        afterWrite?.invoke()
        RefreshScheduler.refreshNow(context)
        return Outcome.Success
    }
}
