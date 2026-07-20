package com.gtdflow.widget.engine

import java.net.URLEncoder

/**
 * Построение deep-link Obsidian `obsidian://open?vault=<имя>&file=<путь>`.
 *
 * Чистый Kotlin (без android.net.Uri) — тестируется на JVM. Кодирование —
 * percent-encoding UTF-8; пробел как %20 (URLEncoder даёт '+', заменяем). Расширение
 * `.md` у пути срезаем: Obsidian резолвит `file` как заметку по пути без расширения
 * (канонический вид его share-ссылок), а с расширением тоже принимает — срез
 * убирает двусмысленность. Имя vault = имя выбранной папки (по умолчанию совпадает
 * с именем vault в Obsidian).
 */
object DeepLink {

    fun open(vaultName: String, filePath: String): String {
        val file = filePath.removeSuffix(".md")
        return "obsidian://open?vault=${enc(vaultName)}&file=${enc(file)}"
    }

    private fun enc(s: String): String =
        URLEncoder.encode(s, Charsets.UTF_8.name())
            .replace("+", "%20")
}
