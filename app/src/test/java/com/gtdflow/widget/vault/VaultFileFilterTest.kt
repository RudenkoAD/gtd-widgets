package com.gtdflow.widget.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Отбор файлов vault для движка: только .md, без служебных/скрытых каталогов (чистый Kotlin). */
class VaultFileFilterTest {

    @Test
    fun keepsMarkdownAtRootAndInFolders() {
        val paths = listOf(
            "Заметка.md",
            "GTD/Работа/Проект.md",
            "Входящие.md",
        )
        assertEquals(paths, VaultFileFilter.selectMarkdown(paths))
    }

    @Test
    fun dropsNonMarkdown() {
        val paths = listOf("картинка.png", "README.txt", "data.json", "Заметка.md")
        assertEquals(listOf("Заметка.md"), VaultFileFilter.selectMarkdown(paths))
    }

    @Test
    fun skipsObsidianTrashGitAndHiddenDirs() {
        val paths = listOf(
            ".obsidian/plugins/gtd-flow/data.json",
            ".obsidian/workspace.md", // .md, но в служебной папке — выкидываем
            ".trash/Удалённое.md",
            ".git/COMMIT_EDITMSG.md",
            ".hidden/Секрет.md", // скрытая папка (ведущая точка)
            "GTD/План.md", // остаётся
        )
        assertEquals(listOf("GTD/План.md"), VaultFileFilter.selectMarkdown(paths))
    }

    @Test
    fun skipsHiddenMarkdownFile() {
        assertFalse(VaultFileFilter.isSelected(".secret.md"))
        assertFalse(VaultFileFilter.isSelected("GTD/.draft.md"))
        assertTrue(VaultFileFilter.isSelected("GTD/draft.md"))
    }

    @Test
    fun caseInsensitiveExtensionAndBlankSegments() {
        assertTrue(VaultFileFilter.isSelected("Заметка.MD"))
        // ведущий/двойной слэш — пустые сегменты игнорируются
        assertTrue(VaultFileFilter.isSelected("/GTD//Проект.md"))
        assertFalse(VaultFileFilter.isSelected(""))
        assertFalse(VaultFileFilter.isSelected("папка/"))
    }

    @Test
    fun predicatesAreConsistent() {
        assertTrue(VaultFileFilter.isSkippedDir(".obsidian"))
        assertTrue(VaultFileFilter.isSkippedDir(".anything"))
        assertFalse(VaultFileFilter.isSkippedDir("GTD"))
        assertTrue(VaultFileFilter.isMarkdown("a.md"))
        assertFalse(VaultFileFilter.isMarkdown("a.markdown"))
    }
}
