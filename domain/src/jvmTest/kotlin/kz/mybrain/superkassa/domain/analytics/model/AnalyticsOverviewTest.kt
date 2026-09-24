package kz.mybrain.superkassa.domain.analytics.model

import kotlinx.datetime.minus
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

    private fun sum(value: String): Long = tiynOf(value)

    private fun summary(revenue: String, receipts: Int = 100, tax: String = "0.00") = SalesSummary(
        receiptCount = receipts,
        revenue = sum(revenue),
        tax = sum(tax)
    )

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
}
