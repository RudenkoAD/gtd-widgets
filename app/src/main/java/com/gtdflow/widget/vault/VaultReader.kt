package com.gtdflow.widget.vault

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.gtdflow.widget.perf.Perf
import java.io.IOException

/** Снимок vault для одного расчёта: карта «путь → содержимое .md» + сырой data.json. */
data class VaultSnapshot(
    val files: Map<String, String>,
    val dataJson: String?,
)

/**
 * Чтение vault через Storage Access Framework.
 *
 * Обход дерева — на прямых запросах DocumentsContract (по одному cursor'у на папку),
 * это на порядок быстрее рекурсии DocumentFile.listFiles() на больших хранилищах.
 * Собираем ТОЛЬКО `.md` (виджету не нужны прочие файлы), папки `.obsidian`,
 * `.trash`, `.git` пропускаем. data.json плагина читаем адресно из
 * `.obsidian/plugins/gtd-flow/data.json`. Путь файла — от корня vault, прямые слэши
 * (тот же формат, что ждёт input.files ядра).
 */
object VaultReader {

    private const val PLUGIN_ID = "gtd-flow"

    private val DIR_MIME = DocumentsContract.Document.MIME_TYPE_DIR
    private val PROJECTION = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    /** Процессный кэш содержимого — перечитываем только изменённые файлы (по mtime). */
    private val cache = VaultContentCache.shared

    /** Счётчики одного обхода vault — для меток GtdPerf (разбивка скана). */
    private class ScanStats {
        var dirs = 0
        var files = 0
        var readMs = 0L
        var cacheHits = 0
        var cacheReads = 0
    }

    /** Полный снимок: .md со всего дерева (минус служебные папки) + data.json плагина. */
    fun read(context: Context, treeUri: Uri): VaultSnapshot {
        cache.bind(treeUri.toString())
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val files = LinkedHashMap<String, String>()
        val stats = ScanStats()
        val startMs = Perf.nowMs()
        collectMarkdown(context, treeUri, rootId, "", files, stats)
        val dataJson = readDataJson(context, treeUri, rootId)
        val totalMs = Perf.nowMs() - startMs
        // enumMs — оценка (обход/курсоры) = всё минус чистое чтение содержимого.
        Perf.mark(
            "scan files=${stats.files} dirs=${stats.dirs}" +
                " enumMs=${totalMs - stats.readMs} readMs=${stats.readMs}" +
                " cacheHits=${stats.cacheHits} cacheReads=${stats.cacheReads}",
        )
        return VaultSnapshot(files, dataJson)
    }

    /** Прочитать ТОЛЬКО data.json плагина (для конфигуратора — без обхода дерева .md). */
    fun readDataJsonOnly(context: Context, treeUri: Uri): String? {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        return readDataJson(context, treeUri, rootId)
    }

    /** Рекурсивно собрать .md в поддереве [parentId] с префиксом пути [prefix]. */
    private fun collectMarkdown(
        context: Context,
        treeUri: Uri,
        parentId: String,
        prefix: String,
        out: MutableMap<String, String>,
        stats: ScanStats,
    ) {
        stats.dirs++
        forEachChild(context, treeUri, parentId) { docId, name, mime, mtime ->
            if (mime == DIR_MIME) {
                // те же предикаты, что и чистый VaultFileFilter (единый источник правды):
                // служебные и скрытые каталоги в движок не попадают
                if (VaultFileFilter.isSkippedDir(name)) return@forEachChild
                collectMarkdown(context, treeUri, docId, join(prefix, name), out, stats)
            } else if (VaultFileFilter.isMarkdown(name) && !name.startsWith(".")) {
                val path = join(prefix, name)
                // Кэш по mtime: неизменённый файл не перечитываем (SAF-IO — узкое место).
                val cached = cache.get(docId, mtime)
                if (cached != null) {
                    stats.cacheHits++
                    stats.files++
                    out[path] = cached
                } else {
                    val readStart = Perf.nowMs()
                    val content = readDocument(context, treeUri, docId)
                    stats.readMs += Perf.nowMs() - readStart
                    if (content != null) {
                        stats.cacheReads++
                        stats.files++
                        out[path] = content
                        cache.put(docId, mtime, content)
                    }
                }
            }
        }
    }

    /** Прочитать `.obsidian/plugins/<PLUGIN_ID>/data.json`, либо null. */
    private fun readDataJson(context: Context, treeUri: Uri, rootId: String): String? {
        val obsidian = childDir(context, treeUri, rootId, ".obsidian") ?: return null
        val plugins = childDir(context, treeUri, obsidian, "plugins") ?: return null
        val plugin = childDir(context, treeUri, plugins, PLUGIN_ID) ?: return null
        val dataId = childFile(context, treeUri, plugin, "data.json") ?: return null
        return readDocument(context, treeUri, dataId)
    }

    // --- примитивы обхода ---

    private inline fun forEachChild(
        context: Context,
        treeUri: Uri,
        parentDocumentId: String,
        body: (docId: String, name: String, mime: String, mtime: Long) -> Unit,
    ) {
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val cursor: Cursor = context.contentResolver.query(childrenUri, PROJECTION, null, null, null)
            ?: return
        cursor.use { c ->
            val idIdx = 0
            val nameIdx = 1
            val mimeIdx = 2
            val mtimeIdx = 3
            while (c.moveToNext()) {
                val docId = c.getString(idIdx) ?: continue
                val name = c.getString(nameIdx) ?: continue
                val mime = c.getString(mimeIdx) ?: ""
                // last_modified бывает null (провайдер не отдал) — тогда 0 (кэш не доверяет).
                val mtime = if (c.isNull(mtimeIdx)) 0L else c.getLong(mtimeIdx)
                body(docId, name, mime, mtime)
            }
        }
    }

    private fun childDir(context: Context, treeUri: Uri, parentId: String, name: String): String? =
        findChild(context, treeUri, parentId, name, dir = true)

    private fun childFile(context: Context, treeUri: Uri, parentId: String, name: String): String? =
        findChild(context, treeUri, parentId, name, dir = false)

    private fun findChild(
        context: Context,
        treeUri: Uri,
        parentId: String,
        name: String,
        dir: Boolean,
    ): String? {
        var found: String? = null
        forEachChild(context, treeUri, parentId) { docId, childName, mime, _ ->
            if (found == null && childName == name && (mime == DIR_MIME) == dir) {
                found = docId
            }
        }
        return found
    }

    private fun readDocument(context: Context, treeUri: Uri, documentId: String): String? {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun join(prefix: String, name: String): String =
        if (prefix.isEmpty()) name else "$prefix/$name"
}
