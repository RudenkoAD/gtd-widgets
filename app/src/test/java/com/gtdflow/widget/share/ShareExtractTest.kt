package com.gtdflow.widget.share

import org.junit.Assert.assertEquals
import org.junit.Test

/** Извлечение строки захвата из экстр ACTION_SEND: тема + URL, URL внутри текста, без URL. */
class ShareExtractTest {

    @Test
    fun subjectPlusUrlFromText() {
        // Браузер шарит: тема = заголовок страницы, текст = URL.
        val line = ShareExtract.buildLine("Заголовок статьи", "https://example.com/article")
        assertEquals("Заголовок статьи https://example.com/article", line)
    }

    @Test
    fun urlInsideTextWithoutSubject() {
        val line = ShareExtract.buildLine(null, "Смотри это https://example.com круто")
        assertEquals("Смотри это круто https://example.com", line)
    }

    @Test
    fun urlOnly() {
        assertEquals("https://example.com", ShareExtract.buildLine(null, "https://example.com"))
    }

    @Test
    fun noUrlUsesSubject() {
        assertEquals("Просто заметка", ShareExtract.buildLine("Просто заметка", null))
    }

    @Test
    fun noUrlNoSubjectUsesText() {
        assertEquals("Купить молоко", ShareExtract.buildLine(null, "Купить молоко"))
    }

    @Test
    fun collapsesNewlinesToOneLine() {
        val line = ShareExtract.buildLine(null, "Заголовок\nhttps://example.com/x")
        assertEquals("Заголовок https://example.com/x", line)
    }

    @Test
    fun subjectPreferredOverInlineText() {
        val line = ShareExtract.buildLine("Моя тема", "любой текст https://example.com/y хвост")
        assertEquals("Моя тема https://example.com/y", line)
    }

    @Test
    fun blankSubjectFallsBackToText() {
        val line = ShareExtract.buildLine("   ", "Текст без ссылки")
        assertEquals("Текст без ссылки", line)
    }

    @Test
    fun bothNullIsEmpty() {
        assertEquals("", ShareExtract.buildLine(null, null))
    }
}
