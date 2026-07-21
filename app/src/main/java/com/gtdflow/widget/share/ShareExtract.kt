package com.gtdflow.widget.share

/**
 * Извлечение строки захвата из экстр ACTION_SEND (ЧИСТО, тестируется на JVM).
 *
 * Правила:
 *  • есть URL в тексте → «<заголовок> <url>» одной строкой, где заголовок — тема
 *    (EXTRA_SUBJECT), а если её нет — текст без самого URL;
 *  • URL нет → заголовок (тема, иначе текст);
 *  • всё схлопывается в одну строку (переносы/повторные пробелы → один пробел).
 *
 * Так шаринг страницы из браузера («Заголовок» + «https://…») превращается в одну
 * осмысленную строку задачи, а произвольный текст остаётся текстом.
 */
object ShareExtract {

    private val URL_REGEX = Regex("""https?://\S+""")
    private val WS_REGEX = Regex("""\s+""")

    fun buildLine(subject: String?, text: String?): String {
        val subj = collapse(subject)
        val body = collapse(text)
        val url = URL_REGEX.find(body)?.value
            ?: return subj.ifBlank { body } // URL нет — заголовок из темы/текста

        val title = subj.ifBlank { collapse(body.replace(url, " ")) }
        return if (title.isBlank()) url else "$title $url"
    }

    private fun collapse(s: String?): String =
        s?.replace(WS_REGEX, " ")?.trim().orEmpty()
}
