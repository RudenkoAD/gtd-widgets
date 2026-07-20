package com.gtdflow.widget.vault

/**
 * Отбор файлов vault для передачи движку — ЧИСТАЯ логика (тестируется на JVM).
 *
 * Ядру нужны только заметки `.md`; служебные и скрытые каталоги vault
 * (`.obsidian`, `.trash`, `.git`, любой сегмент с ведущей точкой) исключаются
 * целиком. Единый источник правды: [VaultReader] при обходе SAF-дерева спрашивает
 * те же предикаты [isSkippedDir]/[isMarkdown], а [selectMarkdown] проверяет уже
 * готовые пути-от-корня — так обход и фильтр не расходятся.
 */
object VaultFileFilter {

    /** Явно служебные каталоги vault (пропускаем даже без ведущей точки). */
    val SKIP_DIRS: Set<String> = setOf(".obsidian", ".trash", ".git")

    /** Каталог пропускается: служебный или скрытый (имя начинается с точки). */
    fun isSkippedDir(name: String): Boolean = name in SKIP_DIRS || name.startsWith(".")

    /** Файл — заметка Markdown (регистр расширения не важен). */
    fun isMarkdown(name: String): Boolean = name.endsWith(".md", ignoreCase = true)

    /**
     * Отфильтровать список путей-от-корня vault (прямые слэши) до тех, что уходят в
     * движок: только `.md`, ни один родительский сегмент не служебный/скрытый и сам
     * файл не скрытый. Пустые сегменты (двойные слэши, ведущий слэш) игнорируются.
     */
    fun selectMarkdown(paths: Iterable<String>): List<String> =
        paths.filter { isSelected(it) }

    /** Проходит ли одиночный путь-от-корня отбор для движка. */
    fun isSelected(path: String): Boolean {
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return false
        val name = segments.last()
        if (!isMarkdown(name) || name.startsWith(".")) return false
        return segments.dropLast(1).none { isSkippedDir(it) }
    }
}
