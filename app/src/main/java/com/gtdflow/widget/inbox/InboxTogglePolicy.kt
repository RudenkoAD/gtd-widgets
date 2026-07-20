package com.gtdflow.widget.inbox

import com.gtdflow.widget.work.EditService

/**
 * Чистые решения чекбокс-тапа «Входящих» (без Context/Glance, покрыто JVM):
 * когда можно оптимистично убрать строку и какую заметку показать после записи.
 *
 * Дефекты, которые закрывает:
 *  • title == null (устаревший экземпляр без параметра): раньше строка оптимистично
 *    исчезала БЕЗ записи в файл — «визуальная ложь», пересчёт молча возвращал её;
 *  • промах записи (файл изменился синком между рендером и тапом): раньше результат
 *    markInboxDone игнорировался, и строка возвращалась без объяснения.
 */
object InboxTogglePolicy {

    /**
     * Оптимистичное удаление допустимо ТОЛЬКО когда есть всё для реальной записи
     * (файл + строка + текст): иначе визуальный эффект без изменения файла.
     */
    fun canAttemptWrite(file: String?, line: Int?, title: String?): Boolean =
        file != null && line != null && title != null

    /**
     * Заметка после попытки записи: промах (строка не найдена/сместилась/IO) —
     * тот же текст, что у шторок правки; успех или отсутствие попытки — null.
     */
    fun noticeAfterWrite(attempted: Boolean, wrote: Boolean): String? =
        if (attempted && !wrote) EditService.FILE_CHANGED else null
}
