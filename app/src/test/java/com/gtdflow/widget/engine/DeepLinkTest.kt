package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Построение obsidian:// deep-link (percent-encoding, срез .md). */
class DeepLinkTest {

    @Test
    fun encodesSpacesAsPercent20() {
        val url = DeepLink.open("My Vault", "a b.md")
        assertEquals("obsidian://open?vault=My%20Vault&file=a%20b", url)
    }

    @Test
    fun stripsMdSuffix() {
        val url = DeepLink.open("V", "GTD/Notes.md")
        assertTrue(url.endsWith("file=GTD%2FNotes"))
    }

    @Test
    fun encodesCyrillic() {
        val url = DeepLink.open("Хранилище", "Заметка.md")
        assertTrue(url.startsWith("obsidian://open?vault=%D0%A5"))
        // '+' из URLEncoder не должно оставаться
        assertTrue(!url.contains("+"))
    }
}
