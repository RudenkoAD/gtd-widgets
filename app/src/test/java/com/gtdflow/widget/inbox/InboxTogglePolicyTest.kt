package com.gtdflow.widget.inbox

import com.gtdflow.widget.work.EditService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Решения чекбокс-тапа «Входящих» (чистый Kotlin, без Android).
 *
 * Дефекты-регрессии: (1) title == null — оптимистичное удаление шло БЕЗ записи в файл
 * («визуальная ложь»); (2) промах записи (файл изменился синком) молча возвращал
 * строку на пересчёте — без заметки пользователю.
 */
class InboxTogglePolicyTest {

    @Test
    fun fullParametersAllowWrite() {
        assertTrue(InboxTogglePolicy.canAttemptWrite("GTD/Inbox.md", 5, "Задача"))
    }

    @Test
    fun nullTitleForbidsOptimisticRemoval() {
        // Регрессия: без title записи не будет — удалять строку из виджета нельзя.
        assertFalse(InboxTogglePolicy.canAttemptWrite("GTD/Inbox.md", 5, null))
    }

    @Test
    fun nullFileOrLineForbidsWrite() {
        assertFalse(InboxTogglePolicy.canAttemptWrite(null, 5, "Задача"))
        assertFalse(InboxTogglePolicy.canAttemptWrite("GTD/Inbox.md", null, "Задача"))
    }

    @Test
    fun missedWriteYieldsFileChangedNotice() {
        // Промах локатора/IO — тот же текст, что у шторок правки.
        assertEquals(
            EditService.FILE_CHANGED,
            InboxTogglePolicy.noticeAfterWrite(attempted = true, wrote = false),
        )
    }

    @Test
    fun successfulWriteYieldsNoNotice() {
        assertNull(InboxTogglePolicy.noticeAfterWrite(attempted = true, wrote = true))
    }

    @Test
    fun noAttemptYieldsNoNotice() {
        // Записи не было (нет доступа/параметров) — заметка о «файл изменился» неуместна.
        assertNull(InboxTogglePolicy.noticeAfterWrite(attempted = false, wrote = false))
    }
}
