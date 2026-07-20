package com.gtdflow.widget.vault

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Приоритет тела виджета при доступном vault. Регрессия дефекта: при сбое расчёта
 * (данных нет) виджет обязан показать ОШИБКУ, а не вечную «Загрузка…».
 */
class WidgetBodyStateTest {

    @Test
    fun noDataNoErrorIsLoading() {
        assertEquals(
            WidgetBodyState.LOADING,
            WidgetBodyState.of(hasData = false, isEmpty = false, hasError = false),
        )
    }

    @Test
    fun noDataWithErrorIsError() {
        assertEquals(
            WidgetBodyState.ERROR,
            WidgetBodyState.of(hasData = false, isEmpty = false, hasError = true),
        )
    }

    @Test
    fun dataWinsOverError() {
        // Есть полезный снимок — показываем его, даже если последний расчёт упал.
        assertEquals(
            WidgetBodyState.DATA,
            WidgetBodyState.of(hasData = true, isEmpty = false, hasError = true),
        )
    }

    @Test
    fun emptyDataIsEmpty() {
        assertEquals(
            WidgetBodyState.EMPTY,
            WidgetBodyState.of(hasData = true, isEmpty = true, hasError = false),
        )
    }
}
