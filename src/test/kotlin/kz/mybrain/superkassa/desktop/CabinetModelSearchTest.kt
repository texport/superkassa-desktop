package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.KkmModel
import kz.mybrain.superkassa.desktop.ui.components.narrowed
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Поиск модели кассы в справочнике ИСНА.
 *
 * Моделей в справочнике сотни, и владелец набирает то, что помнит:
 * часть названия или код. Поиск только по названию заставлял бы искать
 * глазами кассу, у которой владелец знает один код.
 */
class CabinetModelSearchTest {

    private val models = listOf(
        KkmModel(modelCode = "SK-1", name = "Суперкасса 1"),
        KkmModel(modelCode = "SK-2", name = "Суперкасса 2"),
        KkmModel(modelCode = "WEBKASSA-3", name = "Вебкасса Про"),
        KkmModel(modelCode = "NONAME-7", name = null)
    )

    private fun found(query: String): List<String> =
        narrowed(models, query) { listOfNotNull(it.name, it.modelCode) }.map { it.modelCode }

    @Test
    fun `часть названия сужает список`() {
        assertEquals(listOf("SK-1", "SK-2"), found("суперкасс"))
    }

    @Test
    fun `регистр набранного не имеет значения`() {
        assertEquals(found("суперкасс"), found("СУПЕРКАСС"))
    }

    @Test
    fun `модель находится и по коду`() {
        assertEquals(listOf("WEBKASSA-3"), found("webkassa"))
        assertEquals(listOf("NONAME-7"), found("NONAME"))
    }

    @Test
    fun `пустой запрос и пробелы ничего не сужают`() {
        assertEquals(models.map { it.modelCode }, found(""))
        assertEquals(models.map { it.modelCode }, found("   "))
    }

    @Test
    fun `несовпадающий запрос оставляет список пустым`() {
        assertEquals(emptyList(), found("касса у входа"))
    }
}
