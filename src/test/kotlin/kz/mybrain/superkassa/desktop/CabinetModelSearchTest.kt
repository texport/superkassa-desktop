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

    /**
     * Слова набранного ищутся в любом порядке.
     *
     * Владелец набирает то, что помнит, и в том порядке, в каком помнит.
     * Прежде запрос искался целиком, и «1999 Абая» не находило «Магазин
     * на Абая 1999» — владелец читал «ничего не нашлось» о своей точке.
     */
    @Test
    fun `порядок слов не имеет значения`() {
        val places = listOf("Магазин на Абая 1999", "Магазин на Абая 12", "Склад у вокзала")
        fun hits(query: String) = narrowed(places, query) { listOf(it) }

        assertEquals(listOf("Магазин на Абая 1999"), hits("1999 Абая"))
        assertEquals(listOf("Магазин на Абая 1999"), hits("Абая 1999"))
        assertEquals(listOf("Магазин на Абая 1999", "Магазин на Абая 12"), hits("магазин абая"))
        // Запятая прилипает к слову: адрес набирают так, как его читают.
        assertEquals(listOf("Магазин на Абая 12"), hits("Абая, 12"))
        assertEquals(emptyList(), hits("Абая вокзал"))
    }

    /** Слова вправе найтись в разных полях: одно в названии, другое в коде. */
    @Test
    fun `слова ищутся по всем полям сразу`() {
        assertEquals(listOf("WEBKASSA-3"), found("про webkassa"))
        assertEquals(listOf("SK-2"), found("суперкасса sk-2"))
    }
}
