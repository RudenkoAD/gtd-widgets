package com.gtdflow.widget.vault

/**
 * Что рисовать в теле виджета, когда доступ к vault есть ([WidgetVaultGate.READY]).
 * Чистое решение (без Context/Glance), общее для «Сегодня» и «Входящие»,
 * покрыто JVM-тестом.
 *
 * Приоритет: сначала показываем ДАННЫЕ (даже если позже случилась ошибка — на
 * экране остаётся полезный последний снимок); при отсутствии данных ПРЕДПОЧИТАЕМ
 * ТЕКСТ ОШИБКИ загрузочному спиннеру, чтобы виджет не висел вечно на «Загрузка…».
 */
enum class WidgetBodyState {
    /** Есть данные, но список пуст — «Свободно 🎉» / «Входящих нет». */
    EMPTY,

    /** Есть непустые данные — рисуем список. */
    DATA,

    /** Данных нет, но зафиксирована ошибка расчёта — «Ошибка: …» (тап → приложение). */
    ERROR,

    /** Данных и ошибки нет — идёт первый расчёт, «Загрузка…». */
    LOADING,
    ;

    companion object {
        fun of(hasData: Boolean, isEmpty: Boolean, hasError: Boolean): WidgetBodyState = when {
            hasData && isEmpty -> EMPTY
            hasData -> DATA
            hasError -> ERROR
            else -> LOADING
        }
    }
}
