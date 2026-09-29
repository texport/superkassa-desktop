package kz.mybrain.superkassa.domain.cabinet.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Число касс у точки после заведения и переноса кассы здесь же.
 *
 * Касса, заведённая мастером в только что созданной точке, стояла под ней,
 * а точка писала «Касс 0»: число оставалось таким, каким кабинет прислал
 * его со списком точек.
 */
class PlaceRegisterCountTest {

    private val places = listOf(RetailPlace("p1", "Демо 1"), RetailPlace("p2", "Магазин", cashRegisterCount = 2))

    private fun register(place: String?) =
        CabinetRegister(id = "r1", kkmId = 1, status = "DRAFT", retailPlace = place?.let { RetailPlaceRef(it) })

    private fun counts(list: List<RetailPlace>) = list.map { it.cashRegisterCount }

    @Test
    fun `заведённая касса прибавляется к своей точке`() {
        assertEquals(listOf(1L, 2L), counts(places.withRegister(null, register("p1"))))
    }

    @Test
    fun `перенесённая касса уходит из прежней точки в новую`() {
        assertEquals(listOf(1L, 1L), counts(places.withRegister(register("p2"), register("p1"))))
    }

    @Test
    fun `перечитанная на месте касса числа не меняет`() {
        assertEquals(listOf(0L, 2L), counts(places.withRegister(register("p2"), register("p2"))))
    }
}
