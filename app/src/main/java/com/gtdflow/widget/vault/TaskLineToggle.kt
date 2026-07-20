package com.gtdflow.widget.vault

/**
 * Отметка задачи выполненной в тексте файла (чекбокс виджета «Входящие»).
 *
 * Поиск строки — общий [TaskLineLocator] (номер строки → слова title, fallback по
 * единственному совпадению). Переписывается только маркер `[ ]` → `[x]`; остальная
 * строка (эмодзи-поля 📅📍…) и переносы (включая CRLF: '\r' в конце элемента
 * find/replace не трогают) сохраняются дословно.
 */
object TaskLineToggle {

    /**
     * Вернуть новое содержимое с отмеченной задачей, либо null, если подходящей
     * строки не нашлось.
     */
    fun markDone(content: String, oneBasedLine: Int, title: String): String? {
        val lines = content.split("\n").toMutableList()
        val target = TaskLineLocator.locate(lines, oneBasedLine, title) ?: return null
        lines[target] = flip(lines[target])
        return lines.joinToString("\n")
    }

    private fun flip(line: String): String =
        // Regex.replace(input, transform) — заменяет совпадение; строка-цель anchored,
        // поэтому единственный матч. (replaceFirst принимает лишь строку-замену, не лямбду.)
        TaskLineLocator.UNCHECKED_LINE.replace(line) { m -> "${m.groupValues[1]}[x]${m.groupValues[2]}" }
}
