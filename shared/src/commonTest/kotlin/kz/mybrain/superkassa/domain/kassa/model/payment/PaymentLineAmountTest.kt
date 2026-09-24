package kz.mybrain.superkassa.domain.kassa.model.payment

import kz.mybrain.superkassa.domain.kassa.model.refund.RefundAmount
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundProblem
import kz.mybrain.superkassa.domain.kassa.model.refund.refundAmountOf
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Сумма оплаты разбирается теми же правилами, что и всякая сумма кассы.
 *
 * Деньги делятся до тиына и не глубже. Строка оплаты разбирала набранное
 * сама и принимала доли тиына: такая сумма уходила в ОФД вместе с чеком,
 * а остаток второй оплаты считался от неё.
 */
class PaymentLineAmountTest {

    private val total = tenge("1000.00")

    @Test
    fun `доля тиына суммой оплаты не считается`() {
        var split = PaymentSplit()
        split = split.add("CARD")
        split = split.enter(split.entries.lastIndex, "600,999")

        assertNull(split.entries.last().value, "деньги делятся до тиына и не глубже")
        assertEquals(SplitIssue.Empty, split.issue(total), "нерасписанная оплата не даёт пробить чек")
    }

    /** Одно правило на всю кассу: что отвергает сумма возврата, отвергает и оплата. */
    @Test
    fun `сумма оплаты и сумма возврата разбираются одинаково`() {
        var split = PaymentSplit()
        split = split.add("CARD")
        split = split.enter(split.entries.lastIndex, "600,999")

        assertEquals(RefundAmount.Rejected(RefundProblem.NotANumber), refundAmountOf("600,999", 100_000))
        assertNull(split.entries.last().value)
    }

    @Test
    fun `тенге с тиынами разбираются и через запятую, и через точку`() {
        var split = PaymentSplit()
        split = split.add("CARD")

        split = split.enter(split.entries.lastIndex, "600,50")
        assertEquals(tenge("600.50"), split.entries.last().value)
        split = split.enter(split.entries.lastIndex, "600.50")
        assertEquals(tenge("600.50"), split.entries.last().value)
        assertEquals(tenge("399.50"), split.cashSum(total))
        assertNull(split.issue(total))
    }
}
