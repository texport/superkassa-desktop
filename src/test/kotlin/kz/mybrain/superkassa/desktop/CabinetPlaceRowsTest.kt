package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceRow
import kz.mybrain.superkassa.desktop.ui.cabinet.placeRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Колонка торговых точек: что в ней видно и что оставляет поиск.
 *
 * Строки дерева считаются отдельно от разметки, потому что от них зависят
 * обе её раскладки: развёрнутый список и свёрнутый рельс значков берут
 * один и тот же список строк.
 */
class CabinetPlaceRowsTest {

    private val places = listOf(
        place("p1", "Магазин на Абая"),
        place("p2", "Склад у вокзала"),
        place("p3", "Ларёк в парке")
    )

    private val registers = listOf(
        register("r1", "p1", "Касса у входа", "000000000001"),
        register("r2", "p1", null, "000000000002"),
        register("r3", "p2", "Касса склада", "000000000003")
    )

    private fun rows(open: String?, query: String = "") = placeRows(places, registers, open, query)

    private fun names(open: String?, query: String = "") = rows(open, query).map {
        when (it) {
            is PlaceRow.Point -> it.place.name
            is PlaceRow.Register -> it.register.registrationNumber.orEmpty()
        }
    }

    @Test
    fun `без раскрытой точки видны одни точки`() {
        assertEquals(listOf("Магазин на Абая", "Склад у вокзала", "Ларёк в парке"), names(null))
    }

    @Test
    fun `кассы показываются только у раскрытой точки`() {
        assertEquals(
            listOf("Магазин на Абая", "000000000001", "000000000002", "Склад у вокзала", "Ларёк в парке"),
            names("p1")
        )
    }

    @Test
    fun `поиск сужает список до подошедших точек`() {
        assertEquals(listOf("Склад у вокзала"), names(null, "склад"))
        assertEquals(listOf("Склад у вокзала"), names(null, "СКЛАД"))
    }

    @Test
    fun `точка остаётся, когда подошла её касса`() {
        // Владелец ищет кассу по номеру КГД и не обязан помнить точку.
        assertEquals(listOf("Магазин на Абая"), names(null, "000000000002"))
    }

    @Test
    fun `найденная по кассе точка показывает только найденные кассы`() {
        assertEquals(listOf("Магазин на Абая", "000000000002"), names("p1", "000000000002"))
    }

    @Test
    fun `подошедшая своим именем точка показывает все свои кассы`() {
        assertEquals(listOf("Магазин на Абая", "000000000001", "000000000002"), names("p1", "абая"))
    }

    @Test
    fun `ничего не нашлось — список пуст`() {
        assertTrue(rows("p1", "аптека").isEmpty())
    }

    @Test
    fun `ключ строки отличает точку от кассы`() {
        val keys = rows("p1").map { it.id }
        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun `пятьсот точек с кассами собираются в один список`() {
        val many = (1..LARGE).map { place("p$it", "Точка $it") }
        val theirs = many.flatMap { point ->
            (1..PER_PLACE).map { register("${point.id}-$it", point.id, "Касса $it", "$it") }
        }
        val all = placeRows(many, theirs, open = "p7")
        assertEquals(LARGE + PER_PLACE, all.size)
        assertEquals(PER_PLACE, all.count { it is PlaceRow.Register })
    }

    private fun place(id: String, name: String) = RetailPlace(id = id, name = name, cashRegisterCount = 1)

    private fun register(id: String, place: String, name: String?, number: String?) = CabinetRegister(
        id = id,
        kkmId = id.hashCode(),
        internalName = name,
        status = "REGISTERED",
        registrationNumber = number,
        retailPlaceId = place
    )

    private companion object {
        const val LARGE = 500
        const val PER_PLACE = 3
    }
}
