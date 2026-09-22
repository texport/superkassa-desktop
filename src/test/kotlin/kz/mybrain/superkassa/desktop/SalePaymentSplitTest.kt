package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.payment.PaymentSplit
import kz.mybrain.superkassa.desktop.ui.payment.SplitIssue
import kz.mybrain.superkassa.desktop.ui.sale.SaleBlock
import kz.mybrain.superkassa.desktop.ui.sale.SaleState
import kz.mybrain.superkassa.desktop.ui.sale.blockOf
import kz.mybrain.superkassa.desktop.ui.sale.changeOf
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Часть суммы картой, часть наличными — обычный расчёт в магазине.
 *
 * Здесь проверяется то, из-за чего смешанный чек уходит в ОФД неверным:
 * суммы оплат обязаны сложиться в итог до тиына, а сдача считается
 * с наличной части, а не с итога.
 */
class SalePaymentSplitTest {

    private val total = BigDecimal("1000.00")

    @Test
    fun `одна оплата берёт весь итог`() {
        val split = PaymentSplit()
        val payments = split.toPayments(total)
        assertEquals(1, payments.size)
        assertEquals("CASH", payments.first().type)
        assertEquals(total, payments.first().sum)
    }

    /**
     * Смешанный расчёт затевают, когда карты не хватает: набирают
     * карту, а остальное покупатель добирает деньгами. Спросить наличную
     * часть значило бы просить кассира вычесть итог из карты в уме.
     */
    @Test
    fun `наличные забирают остаток, а набирается безналичная часть`() {
        val split = PaymentSplit()
        split.add("CARD")
        split.entries.last().amount = "600.00"
        val payments = split.toPayments(total)
        assertEquals(listOf("CASH", "CARD"), payments.map { it.type })
        assertEquals(BigDecimal("400.00"), payments.first().sum)
        assertEquals(BigDecimal("600.00"), payments.last().sum)
        assertEquals(total, payments.fold(BigDecimal.ZERO) { sum, payment -> sum + payment.sum })
        assertTrue(split.takesRest(split.entries.first()), "остаток берут наличные, а не последняя строка")
    }

    @Test
    fun `без наличных остаток берёт последняя оплата`() {
        val split = PaymentSplit("CARD")
        split.add("ELECTRONIC")
        split.entries.first().amount = "600.00"
        val payments = split.toPayments(total)

        assertEquals(BigDecimal("600.00"), payments.first().sum)
        assertEquals(BigDecimal("400.00"), payments.last().sum)
    }

    @Test
    fun `сумма без остатка не даёт пробить чек`() {
        val split = PaymentSplit()
        split.add("CARD")
        assertEquals(SplitIssue.Empty, split.issue(total))
        split.entries.last().amount = "1000.00"
        assertEquals(SplitIssue.Excess, split.issue(total))
        split.entries.last().amount = "999.99"
        assertNull(split.issue(total))
    }

    /** Набранное кассиром пересчёт не затирает: остаток своего числа не хранит. */
    @Test
    fun `набранная карта остаётся набранной, когда наличные добавлены после неё`() {
        val split = PaymentSplit("CARD")
        split.entries.first().amount = "600.00"
        split.add("CASH")

        assertEquals("600.00", split.entries.first().amount)
        assertEquals(BigDecimal("400.00"), split.cashSum(total))
    }

    @Test
    fun `один и тот же вид оплаты дважды не заводится`() {
        val split = PaymentSplit()
        split.add("CASH")
        assertEquals(1, split.entries.size)
        split.add("CARD")
        split.retype(split.entries.last(), "CASH")
        assertEquals(listOf("CASH", "CARD"), split.types)
    }

    @Test
    fun `сдача считается с наличной части, а не с итога`() {
        val split = PaymentSplit("CARD")
        split.add("CASH")
        split.entries.first().amount = "600.00"
        val cash = split.cashSum(total)
        assertEquals(BigDecimal("400.00"), cash)
        assertEquals(BigDecimal("100.00"), changeOf(BigDecimal("500.00"), cash))
    }

    @Test
    fun `безналичный чек про принятые деньги не спрашивает`() {
        val split = PaymentSplit("CARD")
        assertTrue(!split.hasCash)
        assertEquals(BigDecimal.ZERO, split.cashSum(total))
        val state = SaleState(total = total, taken = BigDecimal("50"), cashSum = BigDecimal.ZERO)
        assertNull(blockOf(state))
    }

    @Test
    fun `нерасписанное разбиение названо причиной отказа`() {
        val state = SaleState(total = total, splitIssue = SplitIssue.Empty)
        assertEquals(SaleBlock.PaymentSplitEmpty, blockOf(state))
        assertEquals(
            SaleBlock.PaymentSplitExcess,
            blockOf(state.copy(splitIssue = SplitIssue.Excess))
        )
    }

    @Test
    fun `последняя оплата не убирается`() {
        val split = PaymentSplit()
        split.remove(split.entries.first())
        assertEquals(1, split.entries.size)
    }

    @Test
    fun `новый чек начинается с одной оплаты`() {
        val split = PaymentSplit()
        split.add("CARD")
        split.entries.last().amount = "600.00"
        split.reset()
        assertEquals(listOf("CASH"), split.types)
        assertEquals(total, split.toPayments(total).single().sum)
    }
}
