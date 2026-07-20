package com.gtdflow.widget.inbox

/**
 * Выбор аргумента `namespace` для GtdWidgetCore.captureTargetPath из метки виджета
 * «Входящие» (ЧИСТО, тестируется на JVM).
 *
 * Захват всегда пишет в КОНКРЕТНОЕ пространство: агрегат «Все» — не цель записи,
 * поэтому откатывается к «Общему» (null). «Общее» тоже → null (ядро трактует null как
 * «Общее»). Именованное пространство передаётся как есть — захват уйдёт в его
 * `<root>/Входящие.md`.
 */
object CaptureNamespace {
    fun forWrite(widgetNamespace: String): String? = when (widgetNamespace) {
        InboxWidgetState.ALL_NAMESPACE, InboxWidgetState.DEFAULT_NAMESPACE -> null
        else -> widgetNamespace
    }
}
