package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.SalesFilter
import kz.mybrain.superkassa.desktop.server.cabinet.SalesPayments
import kz.mybrain.superkassa.desktop.server.cabinet.SalesSummary
import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.ui.analytics.cashlessShare
import kz.mybrain.superkassa.desktop.ui.analytics.overviewOf
import kz.mybrain.superkassa.desktop.ui.analytics.previousFilter
import kz.mybrain.superkassa.desktop.ui.analytics.regionsOf
import kz.mybrain.superkassa.desktop.ui.analytics.sellingRegisters
import kz.mybrain.superkassa.desktop.ui.analytics.silentRegisters
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Счёт итогов для руководства.
 *
 * Здесь проверяется то, на что смотрят первым: изменение к прошлому
 * сроку, доля безналичных и свод по регионам. Ошибка в любом из них
 * показывает падение там, где был рост, — и замечают её не на этом
 * экране, а в разговоре, которому этот экран служит основанием.
 */
class AnalyticsOverviewTest {

    private fun sum(value: String) = BigDecimal(value)

    private fun summary(revenue: String, receipts: Int = 100, tax: String = "0.00") = SalesSummary(
        receiptCount = receipts,
        revenue = sum(revenue),
        tax = sum(tax)
    )

    private val unknown = "Без адреса"

    // --- Изменение к прошлому сроку ---

    @Test
    fun `рост считается целыми процентами`() {
        val overview = overviewOf(summary("150.00", receipts = 120), summary("100.00", receipts = 100))
        assertEquals(50, overview.revenueChange)
        assertEquals(20, overview.receiptsChange)
    }

    @Test
    fun `падение приходит отрицательным числом`() {
        val overview = overviewOf(summary("60.00", receipts = 80), summary("100.00", receipts = 100))
        assertEquals(-40, overview.revenueChange)
        assertEquals(-20, overview.receiptsChange)
    }

    @Test
    fun `прошлого срока нет — изменения нет вовсе`() {
        val overview = overviewOf(summary("150.00"))
        assertNull(overview.revenueChange)
        assertNull(overview.receiptsChange)
        assertNull(overview.taxChange)
        assertNull(overview.cashlessChange)
    }

    @Test
    fun `прошлый срок пуст — изменения нет, а не бесконечный рост`() {
        val overview = overviewOf(summary("150.00", receipts = 10), summary("0.00", receipts = 0))
        assertNull(overview.revenueChange)
        assertNull(overview.receiptsChange)
    }

    @Test
    fun `налог сравнивается своим числом`() {
        val overview = overviewOf(
            summary("150.00", tax = "16.07"),
            summary("100.00", tax = "10.71")
        )
        assertEquals(50, overview.taxChange)
    }

    // --- Доля безналичных ---

    @Test
    fun `доля безналичных складывается из карты, электронных денег и мобильного платежа`() {
        val payments = SalesPayments(
            cash = sum("250.00"),
            card = sum("500.00"),
            electronic = sum("150.00"),
            mobile = sum("100.00")
        )
        assertEquals(75, cashlessShare(payments))
    }

    @Test
    fun `расчётов не было — доли нет, а не ноль процентов`() {
        assertNull(cashlessShare(SalesPayments()))
    }

    @Test
    fun `платили только наличными — доля ноль`() {
        assertEquals(0, cashlessShare(SalesPayments(cash = sum("1000.00"))))
    }

    @Test
    fun `кредит и тара в безналичное не идут`() {
        val payments = SalesPayments(card = sum("500.00"), credit = sum("400.00"), tare = sum("100.00"))
        assertEquals(50, cashlessShare(payments))
    }

    @Test
    fun `изменение доли меряется процентными пунктами`() {
        val now = summary("100.00").copy(payments = SalesPayments(cash = sum("20.00"), card = sum("80.00")))
        val before = summary("100.00").copy(payments = SalesPayments(cash = sum("50.00"), card = sum("50.00")))
        assertEquals(30, overviewOf(now, before).cashlessChange)
    }

    @Test
    fun `прошлый срок без расчётов — доля без сравнения`() {
        val now = summary("100.00").copy(payments = SalesPayments(card = sum("100.00")))
        assertEquals(100, overviewOf(now, summary("0.00")).cashless)
        assertNull(overviewOf(now, summary("0.00")).cashlessChange)
    }

    // --- Кассы на связи и молчащие ---

    private fun register(name: String, receipts: Int, place: String? = null) = SalesUnit(
        id = name,
        name = name,
        retailPlaceName = place,
        receiptCount = receipts,
        revenue = sum(if (receipts > 0) "100.00" else "0.00")
    )

    @Test
    fun `на связи считаются кассы с чеками, молчащие — остальные`() {
        val registers = listOf(register("Первая", 10), register("Вторая", 0), register("Третья", 5))
        val summary = SalesSummary(cashRegisterCount = 5)
        assertEquals(2, sellingRegisters(registers))
        assertEquals(3, silentRegisters(summary, registers))
    }

    @Test
    fun `касса без строки в ответе всё равно считается молчащей`() {
        val summary = SalesSummary(cashRegisterCount = 4)
        assertEquals(4, silentRegisters(summary, emptyList()))
    }

    @Test
    fun `касс в ответе больше, чем в счёте компании — молчащих не отрицательное число`() {
        val registers = listOf(register("Первая", 3), register("Вторая", 4))
        assertEquals(0, silentRegisters(SalesSummary(cashRegisterCount = 1), registers))
    }

    // --- Свод по регионам ---

    private fun place(id: String, name: String, address: String?) =
        RetailPlace(id = id, name = name, address = address)

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

    // --- Прошлый срок ---

    @Test
    fun `прошлый срок той же длины стоит перед нынешним`() {
        val week = SalesFilter(from = LocalDate.parse("2026-09-14"), to = LocalDate.parse("2026-09-20"))
        val before = previousFilter(week)
        assertEquals(LocalDate.parse("2026-09-07"), before.from)
        assertEquals(LocalDate.parse("2026-09-13"), before.to)
    }

    @Test
    fun `для одного дня прошлый срок — вчера`() {
        val day = LocalDate.parse("2026-09-20")
        val before = previousFilter(SalesFilter(from = day, to = day))
        assertEquals(day.minusDays(1), before.from)
        assertEquals(day.minusDays(1), before.to)
    }

    @Test
    fun `отбор по кассе прошлый срок не теряет`() {
        val day = LocalDate.parse("2026-09-20")
        val before = previousFilter(SalesFilter(from = day, to = day, cashRegisterId = "c1"))
        assertEquals("c1", before.cashRegisterId)
    }
}
