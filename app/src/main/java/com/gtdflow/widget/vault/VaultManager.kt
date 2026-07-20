package com.gtdflow.widget.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.gtdflow.widget.data.AppStore

/**
 * Управление доступом к папке vault через Storage Access Framework.
 *
 * Пользователь выбирает корень vault (ACTION_OPEN_DOCUMENT_TREE) в MainActivity;
 * мы берём persistable-разрешение (переживает перезагрузку) и сохраняем tree-URI +
 * отображаемое имя папки (= имя vault в Obsidian по умолчанию, нужно для deep-link)
 * в AppStore. Дальнейшее чтение/запись — по этому URI.
 */
object VaultManager {

    /** Флаги persistable read/write для takePersistableUriPermission. */
    const val PERSIST_FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    /**
     * Закрепить выбранный tree-URI: взять persistable-разрешение, вытащить имя
     * папки и сохранить конфиг. Возвращает отображаемое имя vault (или null, если
     * URI не разрешился в дерево).
     */
    suspend fun persist(context: Context, treeUri: Uri): String? {
        context.contentResolver.takePersistableUriPermission(treeUri, PERSIST_FLAGS)
        val name = DocumentFile.fromTreeUri(context, treeUri)?.name ?: return null
        AppStore.saveVault(context, treeUri.toString(), name)
        return name
    }

    /** Текущий tree-URI из конфига, либо null. */
    suspend fun treeUri(context: Context): Uri? =
        AppStore.vaultConfig(context).treeUri?.let(Uri::parse)

    /**
     * Держим ли ещё persistable-разрешение на сохранённый URI (пользователь мог
     * отозвать доступ или удалить папку). Проверяется по списку постоянных грантов.
     * Требуем и чтение, и запись: все интерактивные действия виджетов (захват,
     * чекбокс, правка из шторки) пишут в vault, поэтому грант «только чтение»
     * фактически бесполезен — честнее сразу отправить выбирать папку заново,
     * чем ронять каждую запись глубоко в стеке с невнятной ошибкой.
     */
    fun hasAccess(context: Context, treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission && it.isWritePermission
        }
}
