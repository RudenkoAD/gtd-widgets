package com.gtdflow.widget.inbox

import com.gtdflow.widget.engine.InboxItem
import com.gtdflow.widget.engine.InboxSection

/**
 * Оптимистичные патчи секции «Входящие» — ЧИСТАЯ логика (тестируется на JVM).
 *
 * Действие в виджете (чекбокс/захват/правка) сначала применяет патч к закэшированной
 * секции и мгновенно перерисовывает виджет, а полный пересчёт из vault идёт следом и
 * заменяет секцию честным результатом. Строка задачи идентифицируется парой
 * файл+номер (1-based) — та же пара, что уходит в запись и в пересчёт.
 */
object InboxOptimistic {

    /** Убрать строку файл+номер (чекбокс/«Выполнено» — задача уходит из входящих). */
    fun removeLine(section: InboxSection, file: String, line: Int): InboxSection =
        section.copy(items = section.items.filterNot { it.file == file && it.line == line })

    /** Обновить текст/место строки файл+номер на месте (правка из шторки). */
    fun editLine(
        section: InboxSection,
        file: String,
        line: Int,
        title: String,
        location: String?,
    ): InboxSection =
        section.copy(
            items = section.items.map {
                if (it.file == file && it.line == line) {
                    it.copy(title = title, location = location?.takeIf(String::isNotBlank))
                } else {
                    it
                }
            },
        )

    /** Добавить элемент в начало списка (захват — новая задача появляется сразу сверху). */
    fun prepend(section: InboxSection, item: InboxItem): InboxSection =
        section.copy(items = listOf(item) + section.items)
}
