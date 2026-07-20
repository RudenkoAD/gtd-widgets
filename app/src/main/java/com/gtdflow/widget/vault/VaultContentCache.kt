package com.gtdflow.widget.vault

/**
 * Процессный кэш содержимого файлов vault: docId → (mtime, content).
 *
 * Узкое место полного скана — чтение содержимого КАЖДОГО `.md` через SAF (десятки мс
 * на файл IPC к DocumentsProvider; на 150+ файлах это секунды, см. метку GtdPerf
 * «scan … readMs=…»). Кэш перечитывает только файлы, у которых вырос last_modified;
 * неизменённые берёт из памяти. После НАШЕЙ записи ([update]) обновляем запись
 * локально — следующий скан не перечитывает изменённый нами файл.
 *
 * ЧИСТАЯ логика (без Android) — тестируется на JVM. Потокобезопасность: доступ к карте
 * сериализуется общим замком; на практике пересчёты и так сериализованы
 * WidgetService.refreshCoalesced, но защищаемся на случай периодики.
 *
 * Ключ — docId (стабильный ID документа SAF в пределах дерева). Версия — mtime (мс);
 * mtime ≤ 0 (провайдер не отдал) считаем «нет доверенной версии» — всегда читаем.
 * Смена vault ([bind] с другим treeUri) сбрасывает кэш целиком (docId не переносимы).
 */
class VaultContentCache {

    private data class Entry(val mtime: Long, val content: String)

    private val lock = Any()
    private val map = HashMap<String, Entry>()
    private var boundTreeUri: String? = null

    /** Привязать кэш к vault. Смена URI — полный сброс (чужие docId невалидны). */
    fun bind(treeUri: String) {
        synchronized(lock) {
            if (boundTreeUri != treeUri) {
                map.clear()
                boundTreeUri = treeUri
            }
        }
    }

    /**
     * Содержимое [docId], если оно закэшировано ИМЕННО с версией [mtime]; иначе null
     * (нет записи, версия устарела или mtime недоверенный ≤ 0 — вызывающий читает файл).
     */
    fun get(docId: String, mtime: Long): String? {
        if (mtime <= 0L) return null
        synchronized(lock) {
            val e = map[docId] ?: return null
            return if (e.mtime == mtime) e.content else null
        }
    }

    /** Положить прочитанное содержимое (mtime ≤ 0 не кэшируем — версия недоверенная). */
    fun put(docId: String, mtime: Long, content: String) {
        if (mtime <= 0L) return
        synchronized(lock) { map[docId] = Entry(mtime, content) }
    }

    /**
     * Локально обновить кэш после нашей записи в файл (docId нам известен). Если новый
     * mtime неизвестен (≤ 0) — инвалидируем запись, чтобы следующий скан перечитал.
     */
    fun update(docId: String, mtime: Long, content: String) {
        synchronized(lock) {
            if (mtime <= 0L) map.remove(docId) else map[docId] = Entry(mtime, content)
        }
    }

    /** Забыть docId (например, файл удалён/перемещён). */
    fun invalidate(docId: String) {
        synchronized(lock) { map.remove(docId) }
    }

    /** Число закэшированных документов (для меток/тестов). */
    fun size(): Int = synchronized(lock) { map.size }

    /** Полный сброс (тесты; отзыв доступа к vault). */
    fun clear() {
        synchronized(lock) {
            map.clear()
            boundTreeUri = null
        }
    }

    companion object {
        /** Единый процессный экземпляр, переживающий отдельные пересчёты. */
        val shared = VaultContentCache()
    }
}
