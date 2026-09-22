package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.payment.PaymentSplit
import kz.mybrain.superkassa.desktop.ui.payment.SplitIssue
import kz.mybrain.superkassa.desktop.ui.returns.RefundAmount
import kz.mybrain.superkassa.desktop.ui.returns.RefundProblem
import kz.mybrain.superkassa.desktop.ui.returns.refundAmountOf
import java.math.BigDecimal
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

    private val total = BigDecimal("1000.00")

    @Test
    fun `доля тиына суммой оплаты не считается`() {
        val split = PaymentSplit()
        split.add("CARD")
        split.entries.last().amount = "600,999"

        assertNull(split.entries.last().value, "деньги делятся до тиына и не глубже")
        assertEquals(SplitIssue.Empty, split.issue(total), "нерасписанная оплата не даёт пробить чек")
    }

    /** Одно правило на всю кассу: что отвергает сумма возврата, отвергает и оплата. */
    @Test
    fun `сумма оплаты и сумма возврата разбираются одинаково`() {
        val split = PaymentSplit()
        split.add("CARD")
        split.entries.last().amount = "600,999"

        assertEquals(RefundAmount.Rejected(RefundProblem.NotANumber), refundAmountOf("600,999", 100_000))
        assertNull(split.entries.last().value)
    }

    @Test
    fun `тенге с тиынами разбираются и через запятую, и через точку`() {
        val split = PaymentSplit()
        split.add("CARD")

        split.entries.last().amount = "600,50"
        assertEquals(0, BigDecimal("600.50").compareTo(split.entries.last().value))
        split.entries.last().amount = "600.50"
        assertEquals(0, BigDecimal("600.50").compareTo(split.entries.last().value))
        assertEquals(0, BigDecimal("399.50").compareTo(split.cashSum(total)))
        assertNull(split.issue(total))
    }
}
