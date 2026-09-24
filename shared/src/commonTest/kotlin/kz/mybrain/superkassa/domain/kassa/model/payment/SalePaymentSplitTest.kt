package kz.mybrain.superkassa.domain.kassa.model.payment

import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleState
import kz.mybrain.superkassa.domain.kassa.model.sale.blockOf
import kz.mybrain.superkassa.domain.kassa.model.sale.changeOf
import kz.mybrain.superkassa.domain.kassa.model.tenge
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

    private val total = tenge("1000.00")

    @Test
    fun `одна оплата берёт весь итог`() {
        var split = PaymentSplit()
        val payments = split.toPayments(total)
        assertEquals(1, payments.size)
        assertEquals("CASH", payments.first().type)
        assertEquals(total, payments.first().sum.let(Tenge::of))
    }

    /**
     * Смешанный расчёт затевают, когда карты не хватает: набирают
     * карту, а остальное покупатель добирает деньгами. Спросить наличную
     * часть значило бы просить кассира вычесть итог из карты в уме.
     */
    @Test
    fun `наличные забирают остаток, а набирается безналичная часть`() {
        var split = PaymentSplit()
        split = split.add("CARD")
        split = split.enter(split.entries.lastIndex, "600.00")
        val payments = split.toPayments(total)
        assertEquals(listOf("CASH", "CARD"), payments.map { it.type })
        assertEquals(tenge("400.00"), payments.first().sum.let(Tenge::of))
        assertEquals(tenge("600.00"), payments.last().sum.let(Tenge::of))
        assertEquals(total, payments.fold(0L) { sum, payment -> sum + payment.sum.let(Tenge::of) })
        assertTrue(split.takesRest(0), "остаток берут наличные, а не последняя строка")
    }

    @Test
    fun `без наличных остаток берёт последняя оплата`() {
        var split = PaymentSplit(listOf(PaymentLine("CARD")))
        split = split.add("ELECTRONIC")
        split = split.enter(0, "600.00")
        val payments = split.toPayments(total)

        assertEquals(tenge("600.00"), payments.first().sum.let(Tenge::of))
        assertEquals(tenge("400.00"), payments.last().sum.let(Tenge::of))
    }

    @Test
    fun `сумма без остатка не даёт пробить чек`() {
        var split = PaymentSplit()
        split = split.add("CARD")
        assertEquals(SplitIssue.Empty, split.issue(total))
        split = split.enter(split.entries.lastIndex, "1000.00")
        assertEquals(SplitIssue.Excess, split.issue(total))
        split = split.enter(split.entries.lastIndex, "999.99")
        assertNull(split.issue(total))
    }

    /** Набранное кассиром пересчёт не затирает: остаток своего числа не хранит. */
    @Test
    fun `набранная карта остаётся набранной, когда наличные добавлены после неё`() {
        var split = PaymentSplit(listOf(PaymentLine("CARD")))
        split = split.enter(0, "600.00")
        split = split.add("CASH")

        assertEquals("600.00", split.entries.first().amount)
        assertEquals(tenge("400.00"), split.cashSum(total))
    }

    @Test
    fun `один и тот же вид оплаты дважды не заводится`() {
        var split = PaymentSplit()
        split = split.add("CASH")
        assertEquals(1, split.entries.size)
        split = split.add("CARD")
        split = split.retype(split.entries.lastIndex, "CASH")
        assertEquals(listOf("CASH", "CARD"), split.types)
    }

    @Test
    fun `сдача считается с наличной части, а не с итога`() {
        var split = PaymentSplit(listOf(PaymentLine("CARD")))
        split = split.add("CASH")
        split = split.enter(0, "600.00")
        val cash = split.cashSum(total)
        assertEquals(tenge("400.00"), cash)
        assertEquals(tenge("100.00"), changeOf(tenge("500.00"), cash))
    }

    @Test
    fun `безналичный чек про принятые деньги не спрашивает`() {
        var split = PaymentSplit(listOf(PaymentLine("CARD")))
        assertTrue(!split.hasCash)
        assertEquals(0L, split.cashSum(total))
        val state = SaleState(total = total, taken = tenge("50"), cashSum = 0L)
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
        var split = PaymentSplit()
        split = split.remove(0)
        assertEquals(1, split.entries.size)
    }

    @Test
    fun `новый чек начинается с одной оплаты`() {
        var split = PaymentSplit()
        split = split.add("CARD")
        split = split.enter(split.entries.lastIndex, "600.00")
        split = split.reset()
        assertEquals(listOf("CASH"), split.types)
        assertEquals(total, split.toPayments(total).single().sum.let(Tenge::of))
    }
}
