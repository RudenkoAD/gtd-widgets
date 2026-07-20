package com.gtdflow.widget.vault

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

/**
 * Точечная запись в vault для чекбокса и захвата.
 *
 * Атомарность — на уровне «прочитал-изменил-записал» одного файла (last-write-wins;
 * конфликты с Remotely Save не решаем — по договорённости зоны). Запись —
 * openOutputStream в режиме «wt» (truncate): содержимое заменяется целиком, хвост
 * старого (более длинного) файла не остаётся.
 */
object VaultWriter {

    private const val MD_MIME = "text/markdown"

    /**
     * Отметить входящую задачу выполненной. Возвращает true, если строка найдена и
     * файл переписан; false — промах (строка не найдена/сместилась → тихий рефреш).
     */
    fun markInboxDone(
        context: Context,
        treeUri: Uri,
        filePath: String,
        oneBasedLine: Int,
        title: String,
    ): Boolean {
        val doc = resolveFile(context, treeUri, filePath) ?: return false
        val content = readText(context, doc.uri) ?: return false
        val next = TaskLineToggle.markDone(content, oneBasedLine, title) ?: return false
        if (next == content) return false
        return writeText(context, doc.uri, next)
    }

    /**
     * Быстрый захват: гарантировать файл входящих (создать помеченным gtd-inbox при
     * отсутствии), дописать строку. Возвращает true при успешной записи.
     */
    fun capture(
        context: Context,
        treeUri: Uri,
        targetPath: String,
        captureLine: String,
    ): Boolean {
        val existing = resolveFile(context, treeUri, targetPath)
        return if (existing == null) {
            val created = createFile(context, treeUri, targetPath) ?: return false
            writeText(context, created.uri, InboxFileText.newFile(captureLine))
        } else {
            // Файл СУЩЕСТВУЕТ: неудача чтения (null) — это НЕ «файл пуст». Раньше здесь
            // стояло `?: ""`, и при IOException у SAF/облачного провайдера (частая
            // ситуация при синке Remotely Save) реальные входящие подменялись пустышкой,
            // а writeText в режиме "wt" (truncate) их безвозвратно стирал. Теперь, как и
            // markInboxDone, прерываемся на null-чтении.
            val next = captureAppend(readText(context, existing.uri), captureLine) ?: return false
            writeText(context, existing.uri, next)
        }
    }

    /**
     * Прочитать один существующий файл vault по пути (для правки строки). null — файл
     * не найден или чтение не удалось (вызывающий прерывается, ничего не пишем).
     */
    fun readFile(context: Context, treeUri: Uri, filePath: String): String? {
        val doc = resolveFile(context, treeUri, filePath) ?: return null
        return readText(context, doc.uri)
    }

    /**
     * Перезаписать существующий файл vault целиком (truncate "wt"). false — файл не
     * найден или запись не удалась.
     */
    fun writeFile(context: Context, treeUri: Uri, filePath: String, text: String): Boolean {
        val doc = resolveFile(context, treeUri, filePath) ?: return false
        return writeText(context, doc.uri, text)
    }

    /**
     * Содержимое для перезаписи СУЩЕСТВУЮЩЕГО файла входящих при захвате (чистая логика,
     * тестируется без Context):
     *  • read == null — чтение не удалось: вернуть null → вызывающий прерывается, файл
     *    не трогаем (иначе перезапись пустышкой стёрла бы записанные ранее задачи);
     *  • read == "" (файл реально пуст) или текст — дописать строку захвата.
     */
    internal fun captureAppend(read: String?, captureLine: String): String? =
        if (read == null) null else InboxFileText.appendCapture(read, captureLine)

    // --- разрешение путей через DocumentFile (единичные файлы — оверхед незначим) ---

    private fun resolveFile(context: Context, treeUri: Uri, path: String): DocumentFile? {
        var dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null
        for (i in 0 until segments.size - 1) {
            dir = dir.findFile(segments[i])?.takeIf { it.isDirectory } ?: return null
        }
        return dir.findFile(segments.last())?.takeIf { it.isFile }
    }

    /** Создать файл по пути, создавая недостающие папки. null — не удалось. */
    private fun createFile(context: Context, treeUri: Uri, path: String): DocumentFile? {
        var dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null
        for (i in 0 until segments.size - 1) {
            val name = segments[i]
            dir = dir.findFile(name)?.takeIf { it.isDirectory }
                ?: dir.createDirectory(name)
                ?: return null
        }
        val leaf = segments.last()
        // повторно проверяем гонку: файл мог появиться
        dir.findFile(leaf)?.let { return it }
        return dir.createFile(MD_MIME, leaf)
    }

    private fun readText(context: Context, uri: Uri): String? =
        try {
            context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (_: IOException) {
            null
        }

    private fun writeText(context: Context, uri: Uri, text: String): Boolean =
        try {
            // «wt» — усечь перед записью: провайдер не оставит хвост старого содержимого
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
                true
            }?.also {
                // Инвалидируем кэш для НАШЕГО же файла: mtime провайдера мог не измениться
                // в пределах секунды, а скан не должен вернуть старое содержимое.
                runCatching { VaultContentCache.shared.invalidate(DocumentsContract.getDocumentId(uri)) }
            } ?: false
        } catch (_: IOException) {
            false
        }
}
