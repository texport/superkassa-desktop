package kz.mybrain.superkassa.presentation.kassa.sale.position

import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.kassa.unitShort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Единицы измерения на языке кассира.
 *
 * Перечень берётся у ядра, а слова — свои по коду: у ядра они только
 * русские и казахские, и английский интерфейс показывал «шт».
 */
class MeasureUnitTest {

    private val known = UnitOfMeasurement.entries.filter { it != UnitOfMeasurement.UNKNOWN }

    @Test
    fun `у каждой единицы ядра есть слово на каждом языке`() {
        Language.entries.forEach { language ->
            val missing = known.filter { unitShort(it.code, language).isNullOrBlank() }
            assertTrue(missing.isEmpty(), "$language: нет слова для ${missing.map { it.code }}")
        }
    }

    @Test
    fun `английский интерфейс говорит по-английски`() {
        val english = measureUnits(Language.En).associate { it.code to it.title }

        assertEquals("pcs", english.getValue("796"))
        assertEquals("kg", english.getValue("116"))
        assertEquals("дана", unitTitle(measureUnits(Language.Kk), "796"))
        assertEquals("шт", unitTitle(measureUnits(Language.Ru), "796"))
        val cyrillic = known.filter { english.getValue(it.code).any { char -> char in 'а'..'я' } }
        assertTrue(cyrillic.isEmpty(), "в английском осталась кириллица: $cyrillic")
    }

    @Test
    fun `незнакомый код показывается как есть, пустой — без единицы`() {
        assertNull(unitShort("999", Language.En))
        assertEquals("999", unitTitle(measureUnits(Language.En), "999"))
        assertEquals("", unitTitle(measureUnits(Language.En), null))
    }
}
