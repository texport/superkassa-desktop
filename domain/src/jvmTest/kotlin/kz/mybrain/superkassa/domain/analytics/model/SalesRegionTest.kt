package kz.mybrain.superkassa.domain.analytics.model

import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Свод торговых точек по регионам: регион из адреса точки, доли в целое,
 * точка без адреса и точка вне справочника не теряются.
 */
class SalesRegionTest {

    private val unknown = "Без адреса"

    private fun sum(value: String): Long = tiynOf(value)

    private fun register(name: String, receipts: Int, place: String? = null) = SalesUnit(
        id = name,
        name = name,
        retailPlaceName = place,
        receiptCount = receipts,
        revenue = sum(if (receipts > 0) "100.00" else "0.00")
    )

    // --- Свод по регионам ---

    private fun place(id: String, name: String, address: String?) =
        PlaceAddress(id, name, address)

    private fun placeRow(id: String, name: String, receipts: Int, revenue: String) =
        SalesUnit(id = id, name = name, receiptCount = receipts, revenue = sum(revenue))

    private val catalogue = listOf(
        place("p1", "Магазин на Абая", "Алматы, Алмалинский район, Абая, 10"),
        place("p2", "Магазин у вокзала", "Алматы, Жетысуский район, Сейфуллина, 4"),
        place("p3", "Павильон в Астане", "Астана, Есильский район, Кунаева, 12"),
        place("p4", "Лавка без адреса", null)
    )

    private val rows = listOf(
        placeRow("p1", "Магазин на Абая", 100, "300.00"),
        placeRow("p2", "Магазин у вокзала", 50, "100.00"),
        placeRow("p3", "Павильон в Астане", 80, "500.00"),
        placeRow("p4", "Лавка без адреса", 10, "100.00")
    )

    @Test
    fun `регион берётся из адреса точки, а точки складываются вместе`() {
        val regions = regionsOf(rows, emptyList(), catalogue, unknown)
        val almaty = regions.single { it.title == "Алматы" }
        assertEquals(2, almaty.placeCount)
        assertEquals(150, almaty.receiptCount)
        assertEquals(sum("400.00"), almaty.revenue)
    }

    @Test
    fun `регионы идут по убыванию выручки`() {
        val regions = regionsOf(rows, emptyList(), catalogue, unknown)
        assertEquals(listOf("Астана", "Алматы", unknown), regions.map { it.title })
    }

    @Test
    fun `точка без адреса не теряется, а названа словами`() {
        val regions = regionsOf(rows, emptyList(), catalogue, unknown)
        val nameless = regions.single { it.title == unknown }
        assertEquals(1, nameless.placeCount)
        assertEquals(sum("100.00"), nameless.revenue)
    }

    @Test
    fun `доли регионов складываются в целое`() {
        val regions = regionsOf(rows, emptyList(), catalogue, unknown)
        assertEquals(listOf(50, 40, 10), regions.map { it.percent })
    }

    @Test
    fun `в регионе считаются кассы с чеками, а молчащие в счёт не идут`() {
        val registers = listOf(
            register("Касса 1", 60, place = "Магазин на Абая"),
            register("Касса 2", 40, place = "Магазин у вокзала"),
            register("Касса 3", 0, place = "Магазин у вокзала"),
            register("Касса 4", 80, place = "Павильон в Астане")
        )
        val regions = regionsOf(rows, registers, catalogue, unknown)
        assertEquals(2, regions.single { it.title == "Алматы" }.registerCount)
        assertEquals(1, regions.single { it.title == "Астана" }.registerCount)
        assertEquals(0, regions.single { it.title == unknown }.registerCount)
    }

    @Test
    fun `точки нет в справочнике — регион не выдумывается`() {
        val regions = regionsOf(listOf(placeRow("p9", "Чужая", 5, "50.00")), emptyList(), catalogue, unknown)
        assertEquals(unknown, regions.single().title)
    }

    @Test
    fun `выручки нет ни у одной точки — доли нулевые, а строки остаются`() {
        val quiet = listOf(placeRow("p1", "Магазин на Абая", 0, "0.00"))
        val regions = regionsOf(quiet, emptyList(), catalogue, unknown)
        assertEquals(0, regions.single().percent)
    }
}
