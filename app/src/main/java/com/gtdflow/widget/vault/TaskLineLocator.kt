package com.gtdflow.widget.vault

/**
 * Поиск строки-задачи (незакрытого чекбокса) в тексте файла по данным виджета:
 * номер строки (1-based из ядра) + заголовок (Task.description). Общая логика для
 * отметки выполненной ([TaskLineToggle]) и правки текста задачи ([TaskLineEdit]).
 *
 * СНАЧАЛА доверяем НОМЕРУ строки: это тот же разбор, что дал title, поэтому номер —
 * самый сильный сигнал. Принимаем его, если это незакрытый чекбокс, чьи слова
 * согласуются с title. Сравнение — ПО СЛОВАМ (title приходит со схлопнутыми пробелами
 * и вырезанными инлайновыми полями 📅/📍/⏫, поэтому не обязан быть подстрокой сырой
 * строки): слова title должны идти подпоследовательностью среди слов строки. Fallback
 * (файл сместился синком) — ТОЛЬКО при ЕДИНСТВЕННОМ совпадении, иначе null (не угадываем).
 */
object TaskLineLocator {

    /** Список-строка с ПУСТЫМ чекбоксом: отступ+буллет, `[ ]`, произвольный остаток. */
    val UNCHECKED_LINE = Regex("""^(\s*[-*+] )\[ ](.*)$""")
    private val WHITESPACE = Regex("""\s+""")

    /** Индекс (0-based) строки для операции, либо null, если подходящей нет. */
    fun locate(lines: List<String>, oneBasedLine: Int, title: String): Int? {
        val needle = words(title)
        val byNumber = oneBasedLine - 1
        val atNumber = contentWords(lines.getOrNull(byNumber))
        if (atNumber != null && isWordSubsequence(needle, atNumber)) return byNumber

        if (needle.isEmpty()) return null // без слов матчить по тексту нельзя
        val candidates = lines.indices.filter { i ->
            contentWords(lines[i])?.let { isWordSubsequence(needle, it) } == true
        }
        return candidates.singleOrNull()
    }

    /** Слова содержимого незакрытого чекбокса, либо null, если строка — не такой чекбокс. */
    fun contentWords(line: String?): List<String>? {
        // find (не matchEntire): якорь ^ ищет с начала, хвостовой '\r' от CRLF остаётся
        // за пределами совпадения (. не берёт разделители строк).
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
        return i == needle.size // пустой needle → true (матч по одному номеру строки)
    }
}
