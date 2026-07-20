package com.gtdflow.widget.vault

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
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
    )

    /** Полный снимок: .md со всего дерева (минус служебные папки) + data.json плагина. */
    fun read(context: Context, treeUri: Uri): VaultSnapshot {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val files = LinkedHashMap<String, String>()
        collectMarkdown(context, treeUri, rootId, "", files)
        val dataJson = readDataJson(context, treeUri, rootId)
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
    ) {
        forEachChild(context, treeUri, parentId) { docId, name, mime ->
            if (mime == DIR_MIME) {
                // те же предикаты, что и чистый VaultFileFilter (единый источник правды):
                // служебные и скрытые каталоги в движок не попадают
                if (VaultFileFilter.isSkippedDir(name)) return@forEachChild
                collectMarkdown(context, treeUri, docId, join(prefix, name), out)
            } else if (VaultFileFilter.isMarkdown(name) && !name.startsWith(".")) {
                val path = join(prefix, name)
                readDocument(context, treeUri, docId)?.let { out[path] = it }
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
        body: (docId: String, name: String, mime: String) -> Unit,
    ) {
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val cursor: Cursor = context.contentResolver.query(childrenUri, PROJECTION, null, null, null)
            ?: return
        cursor.use { c ->
            val idIdx = 0
            val nameIdx = 1
            val mimeIdx = 2
            while (c.moveToNext()) {
                val docId = c.getString(idIdx) ?: continue
                val name = c.getString(nameIdx) ?: continue
                val mime = c.getString(mimeIdx) ?: ""
                body(docId, name, mime)
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
        forEachChild(context, treeUri, parentId) { docId, childName, mime ->
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
