package com.gtdflow.widget.vault

/**
 * Замена одной строки файла на отредактированную (шторка события / оверлей задачи).
 * ЧИСТАЯ текстовая логика — тестируется без Context.
 *
 * Два режима нахождения цели:
 *  • [anchorExact] — по номеру + ТОЧНОЙ исходной строке (rawLine из ядра). Для ленты
 *    «Сегодня»/«Агенды»: элемент может быть событием, а не чекбоксом, поэтому якорь —
 *    вся строка целиком. Несовпадение (файл изменился синком) → null, писать нельзя.
 *  • [locate] — по номеру + словам title (через [TaskLineLocator]). Для «Входящих»,
 *    где ядро не отдаёт rawLine: находим незакрытый чекбокс, а его сырой текст сами
 *    берём из файла (см. [LocatedLine.rawLine]) как аргумент buildEditedLine.
 *
 * Хвостовой '\r' (CRLF) сохраняется при замене; сравнение — по «голой» строке без '\r'.
 */
object TaskLineEdit {

    /** Найденная строка: индекс (0-based) и её «голый» текст (без хвостового '\r'). */
    data class LocatedLine(val index: Int, val rawLine: String)

    /**
     * Индекс строки, ТОЧНО равной [exactRawLine] (с точностью до хвостового '\r'), по
     * номеру [oneBasedLine]; null — если номер вне диапазона или строка не совпала.
     */
    fun anchorExact(content: String, oneBasedLine: Int, exactRawLine: String): Int? {
        val lines = content.split("\n")
        val idx = oneBasedLine - 1
        if (idx < 0 || idx >= lines.size) return null
        return if (bare(lines[idx]) == exactRawLine) idx else null
    }

    /** Найти незакрытый чекбокс по номеру+title и вернуть его индекс и сырой текст. */
    fun locate(content: String, oneBasedLine: Int, title: String): LocatedLine? {
        val lines = content.split("\n")
        val idx = TaskLineLocator.locate(lines, oneBasedLine, title) ?: return null
        return LocatedLine(idx, bare(lines[idx]))
    }

    /**
     * Заменить строку [index] на [newLine], сохранив исходный хвостовой '\r'. Индекс
     * обязан быть валидным (получен из [anchorExact]/[locate] на ТОМ ЖЕ content).
     */
    fun replaceAt(content: String, index: Int, newLine: String): String {
        val lines = content.split("\n").toMutableList()
        val hadCr = lines[index].endsWith("\r")
        lines[index] = if (hadCr) "$newLine\r" else newLine
        return lines.joinToString("\n")
    }

    private fun bare(line: String): String = if (line.endsWith("\r")) line.dropLast(1) else line
}
