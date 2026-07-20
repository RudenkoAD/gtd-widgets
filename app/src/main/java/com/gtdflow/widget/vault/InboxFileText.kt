package com.gtdflow.widget.vault

/**
 * Текстовые преобразования файла входящих для быстрого захвата (семантика
 * ensureCaptureFileNs плагина + append строки-задачи).
 *
 * ensureCaptureFileNs гарантирует `gtd-inbox: true` во frontmatter файла-цели, затем
 * строка-задача дописывается в конец. gtd-namespace-override здесь НЕ нужен:
 * captureTargetPath ядра всегда возвращает КОНВЕНЦИОННЫЙ путь (`<root>/Входящие.md`
 * либо `<commonRoot>/Входящие.md`), который в своё пространство попадает по ПАПКЕ —
 * needsOverride в ensureCaptureFileNs для таких путей всегда false.
 */
object InboxFileText {

    /** frontmatter строго в начале файла: «---\n…\n---» + перевод строки/конец. */
    private val FRONTMATTER = Regex("""^---\r?\n([\s\S]*?)\r?\n---[ \t]*(\r?\n|$)""")
    private val INBOX_KEY = Regex("""^gtd-inbox:""")
    private val INBOX_TRUE = Regex("""^gtd-inbox:\s*true\s*$""", RegexOption.IGNORE_CASE)

    /** Содержимое нового файла входящих: frontmatter-флаг + пустая строка + захват. */
    fun newFile(captureLine: String): String =
        "---\ngtd-inbox: true\n---\n\n$captureLine\n"

    /**
     * Дописать строку захвата в конец файла, предварительно гарантировав флаг
     * `gtd-inbox: true` во frontmatter. Итог всегда завершается одним переводом строки.
     */
    fun appendCapture(content: String, captureLine: String): String {
        val withFlag = ensureInboxFrontmatter(content)
        val trimmed = withFlag.trimEnd('\n', '\r')
        return if (trimmed.isEmpty()) "$captureLine\n" else "$trimmed\n$captureLine\n"
    }

    /**
     * Гарантировать `gtd-inbox: true` во frontmatter (идемпотентно):
     *  • нет frontmatter → добавить блок в начало (контент сохраняется ниже);
     *  • есть, ключ отсутствует → вставить строку сразу после открывающего `---`;
     *  • есть, ключ есть но не `true` → заменить его значение на `true`;
     *  • есть, ключ уже `true` → без изменений.
     */
    fun ensureInboxFrontmatter(content: String): String {
        val m = FRONTMATTER.find(content) ?: return prependFrontmatter(content)

        val bodyLines = m.groupValues[1].split("\n")
        val hasTrue = bodyLines.any { INBOX_TRUE.matches(it.trim()) }
        if (hasTrue) return content

        val keyIdx = bodyLines.indexOfFirst {
            INBOX_KEY.containsMatchIn(it) && !it.startsWith(" ") && !it.startsWith("\t")
        }
        val newBody = if (keyIdx >= 0) {
            bodyLines.toMutableList().apply { this[keyIdx] = "gtd-inbox: true" }.joinToString("\n")
        } else if (m.groupValues[1].isEmpty()) {
            "gtd-inbox: true"
        } else {
            "gtd-inbox: true\n${m.groupValues[1]}"
        }
        // пересобрать блок, сохранив хвост после закрывающего «---» дословно
        val tail = content.substring(m.range.last + 1) // всё после совпадения frontmatter
        val closingNewline = m.groupValues[2] // «\n» / «\r\n» / пусто
        return "---\n$newBody\n---$closingNewline$tail"
    }

    private fun prependFrontmatter(content: String): String {
        val fm = "---\ngtd-inbox: true\n---\n"
        return if (content.isBlank()) fm else "$fm\n$content"
    }
}
