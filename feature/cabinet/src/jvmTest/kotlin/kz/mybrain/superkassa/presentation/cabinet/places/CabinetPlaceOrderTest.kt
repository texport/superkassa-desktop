package kz.mybrain.superkassa.presentation.cabinet.places

import kz.mybrain.superkassa.presentation.cabinet.places.SieveScene.allPlaces
import kz.mybrain.superkassa.presentation.cabinet.places.SieveScene.names
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Порядок колонки торговых точек: по названию, адресу, числу касс и состоянию.
 *
 * Порядок только переставляет строки: сузить список он не должен, иначе
 * владелец принимал бы перестановку за потерю точек.
 */
class CabinetPlaceOrderTest {

    @Test
    fun `порядок по названию идёт в обе стороны`() {
        assertEquals(
            listOf("Ларёк в парке", "Магазин на Абая", "Склад у вокзала"),
            names(PlaceSieve(order = PlaceOrder.Name))
        )
        assertEquals(
            listOf("Склад у вокзала", "Магазин на Абая", "Ларёк в парке"),
            names(PlaceSieve(order = PlaceOrder.Name, descending = true))
        )
    }

    @Test
    fun `порядок по адресу идёт в обе стороны`() {
        assertEquals(
            listOf("Магазин на Абая", "Склад у вокзала", "Ларёк в парке"),
            names(PlaceSieve(order = PlaceOrder.Address))
        )
        assertEquals(
            listOf("Ларёк в парке", "Склад у вокзала", "Магазин на Абая"),
            names(PlaceSieve(order = PlaceOrder.Address, descending = true))
        )
    }

    @Test
    fun `порядок по числу касс идёт в обе стороны`() {
        assertEquals(
            listOf("Склад у вокзала", "Ларёк в парке", "Магазин на Абая"),
            names(PlaceSieve(order = PlaceOrder.Registers))
        )
        assertEquals(
            listOf("Магазин на Абая", "Ларёк в парке", "Склад у вокзала"),
            names(PlaceSieve(order = PlaceOrder.Registers, descending = true))
        )
    }

    /**
     * Порядок по состоянию ставит первым то, во что надо вмешаться.
     *
     * Отказ КГД — работа на сегодня, поданное заявление — ожидание,
     * снятая с учёта касса не спрашивает ни о чём. Обратная сторона
     * нужна не для симметрии: так владелец видит, что у него уже сделано.
     */
    @Test
    fun `порядок по состоянию ведёт от беды к покою`() {
        assertEquals(
            listOf("Магазин на Абая", "Ларёк в парке", "Склад у вокзала"),
            names(PlaceSieve(order = PlaceOrder.Record))
        )
        assertEquals(
            listOf("Склад у вокзала", "Ларёк в парке", "Магазин на Абая"),
            names(PlaceSieve(order = PlaceOrder.Record, descending = true))
        )
    }

    /** Порядок ничего не убирает: сколько точек было, столько и осталось. */
    @Test
    fun `порядок не сужает список`() {
        PlaceOrder.entries.forEach { order ->
            assertEquals(allPlaces.size, names(PlaceSieve(order = order)).size, "$order: список стал короче")
        }
    }
}
