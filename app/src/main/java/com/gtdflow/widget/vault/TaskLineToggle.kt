package com.gtdflow.widget.vault

/**
 * Отметка задачи выполненной в тексте файла (чекбокс виджета «Входящие»).
 *
 * Данных виджета достаточно для устойчивого поиска строки. СНАЧАЛА пробуем СТРОКУ
 * ПО НОМЕРУ (1-based line из ядра): это тот же разбор, что дал title, поэтому номер —
 * самый сильный сигнал. Доверяем ему, если это незакрытый чекбокс, чей текст
 * согласуется с title.
 *
 * ВАЖНО про сравнение текста: title приходит из ядра как Task.description — со
 * СХЛОПНУТЫМИ пробелами (`\s+`→` `) и ВЫРЕЗАННЫМИ инлайновыми полями (📅/📍/приоритет
 * ⏫ и пр.). Поэтому description НЕ обязан быть подстрокой сырой строки файла. Прямой
 * `line.contains(description)` давал ложные промахи (двойной пробел; поле между слов),
 * из-за чего быстрый путь проваливался в fallback и мог отметить ЧУЖУЮ задачу. Теперь
 * сверяем по СЛОВАМ: слова title должны идти подпоследовательностью среди слов строки
 * (лишние поля-токены просто пропускаются). Fallback (файл сместился синком) отмечает
 * ТОЛЬКО при ЕДИНСТВЕННОМ совпадении — иначе тихо ничего не делаем (виджет обновится).
 *
 * Переписывается только маркер `[ ]` → `[x]`; остальная строка (эмодзи-поля 📅📍…)
 * и переносы строк сохраняются дословно (split/join одним '\n' без потерь, включая
 * CRLF — '\r' остаётся в конце элемента: find/replace его не трогают).
 */
object TaskLineToggle {

    /** Список-строка с ПУСТЫМ чекбоксом: отступ+буллет, `[ ]`, произвольный остаток. */
    private val UNCHECKED_LINE = Regex("""^(\s*[-*+] )\[ ](.*)$""")
    private val WHITESPACE = Regex("""\s+""")

    /**
     * Вернуть новое содержимое с отмеченной задачей, либо null, если подходящей
     * строки не нашлось.
     */
    fun markDone(content: String, oneBasedLine: Int, title: String): String? {
        val lines = content.split("\n").toMutableList()
        val target = locate(lines, oneBasedLine, title) ?: return null
        lines[target] = flip(lines[target])
        return lines.joinToString("\n")
    }

    /** Индекс строки для отметки: быстрый путь по номеру, затем поиск по тексту. */
    private fun locate(lines: List<String>, oneBasedLine: Int, title: String): Int? {
        val needle = words(title)
        val byNumber = oneBasedLine - 1
        // Быстрый путь: доверяем номеру, если это незакрытый чекбокс, чьи слова
        // согласуются с title (пустой title — матчим по одному номеру).
        val atNumber = contentWords(lines.getOrNull(byNumber))
        if (atNumber != null && isWordSubsequence(needle, atNumber)) return byNumber

        if (needle.isEmpty()) return null // без слов матчить по тексту нельзя (см. выше)
        // Fallback: файл сместился — берём совпадение по словам, но лишь ОДНОЗНАЧНОЕ.
        val candidates = lines.indices.filter { i ->
            contentWords(lines[i])?.let { isWordSubsequence(needle, it) } == true
        }
        return candidates.singleOrNull()
    }

    /** Слова содержимого незакрытого чекбокса, либо null, если строка — не такой чекбокс. */
    private fun contentWords(line: String?): List<String>? {
        // find (не matchEntire): якорь ^ ищет с начала, а хвостовой '\r' от CRLF
        // остаётся за пределами совпадения (. не берёт разделители строк).
        val m = line?.let { UNCHECKED_LINE.find(it) } ?: return null
        return words(m.groupValues[2])
    }

    private fun words(s: String): List<String> =
        s.trim().split(WHITESPACE).filter { it.isNotEmpty() }

    /** Все слова needle встречаются в haystack в исходном порядке (лишние — пропускаются). */
    private fun isWordSubsequence(needle: List<String>, haystack: List<String>): Boolean {
        var i = 0
        for (w in haystack) {
            if (i < needle.size && w == needle[i]) i++
        }
        return i == needle.size // пустой needle → 0==0 → true (матч по номеру строки)
    }

    private fun flip(line: String): String =
        // Regex.replace(input, transform) — заменяет совпадение; строка-цель anchored,
        // поэтому единственный матч. (replaceFirst принимает лишь строку-замену, не лямбду.)
        UNCHECKED_LINE.replace(line) { m -> "${m.groupValues[1]}[x]${m.groupValues[2]}" }
}
