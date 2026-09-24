package kz.mybrain.superkassa.presentation.analytics.sales

import kotlinx.datetime.plus
import kz.mybrain.superkassa.presentation.strings.analytics.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Столбцы таблиц сводки: у таблицы точек нет столбцов кассы, выстраивают таблицу только числа. */
class AnalyticsSalesColumnsTest {

    // --- Столбцы таблиц ---

    @Test
    fun `у таблицы точек нет столбцов кассы`() {
        val places = salesColumns(SalesRows.Places)
        assertTrue(SalesColumn.RegistrationNumber !in places, places.joinToString())
        assertTrue(SalesColumn.RetailPlace !in places, places.joinToString())
        assertEquals(salesColumns(SalesRows.Registers).size - 2, places.size)
    }

    @Test
    fun `у таблицы касс столбцы кассы на месте и подписаны по-своему`() {
        val whole = analyticsTexts(Language.Ru)
        val registers = salesColumns(SalesRows.Registers)
        assertTrue(SalesColumn.RegistrationNumber in registers)
        assertEquals(whole.sales.colName, salesColumnTitle(SalesColumn.Name, SalesRows.Registers, whole))
        assertEquals(whole.retailPlace, salesColumnTitle(SalesColumn.Name, SalesRows.Places, whole))
    }

    @Test
    fun `выстраивают таблицу только числовые столбцы`() {
        assertEquals(SalesOrder.Revenue, salesSortOrder(SalesColumn.Revenue))
        assertEquals(SalesOrder.Receipts, salesSortOrder(SalesColumn.Receipts))
        assertNull(salesSortOrder(SalesColumn.Name))
        assertNull(salesSortOrder(SalesColumn.LastContact))
    }
}
