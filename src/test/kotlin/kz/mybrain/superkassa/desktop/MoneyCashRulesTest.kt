package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.cash.CashAttempt
import kz.mybrain.superkassa.desktop.ui.cash.CashDecision
import kz.mybrain.superkassa.desktop.ui.cash.CashMove
import kz.mybrain.superkassa.desktop.ui.cash.CashRefusal
import kz.mybrain.superkassa.desktop.ui.cash.CashRules
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Правила движения наличных.
 *
 * Узел проверяет то же самое, но кассир стоит перед покупателем: причина
 * должна быть названа до обращения к серверу, а не отказом после.
 */
class MoneyCashRulesTest {

    private val open = true

    private fun refusal(decision: CashDecision): CashRefusal? = (decision as? CashDecision.Refused)?.reason

    @Test
    fun `пустое поле ошибкой не считается`() {
        assertTrue(CashRules.check("", CashMove.Deposit, DRAWER, open) is CashDecision.Empty)
        assertTrue(CashRules.check("   ", CashMove.Withdraw, DRAWER, open) is CashDecision.Empty)
    }

    @Test
    fun `не число суммой не считается`() {
        assertEquals(CashRefusal.NotANumber, refusal(CashRules.check("тысяча", CashMove.Deposit, DRAWER, open)))
        assertEquals(CashRefusal.NotANumber, refusal(CashRules.check("10.005", CashMove.Deposit, DRAWER, open)))
    }

    @Test
    fun `ноль и отрицательное не проводятся`() {
        assertEquals(CashRefusal.NotPositive, refusal(CashRules.check("0", CashMove.Deposit, DRAWER, open)))
        assertEquals(CashRefusal.NotPositive, refusal(CashRules.check("0,00", CashMove.Withdraw, DRAWER, open)))
        assertEquals(CashRefusal.NotPositive, refusal(CashRules.check("-5", CashMove.Deposit, DRAWER, open)))
    }

    @Test
    fun `лишний ноль в сумме останавливается пределом операции`() {
        assertEquals(CashRefusal.TooLarge, refusal(CashRules.check("100000000", CashMove.Deposit, DRAWER, open)))
        assertTrue(CashRules.check("99999999,99", CashMove.Deposit, DRAWER, open) is CashDecision.Ready)
    }

    @Test
    fun `при закрытой смене деньги не двигаются`() {
        assertEquals(CashRefusal.ShiftClosed, refusal(CashRules.check("100", CashMove.Deposit, DRAWER, false)))
        assertEquals(CashRefusal.ShiftClosed, refusal(CashRules.check("100", CashMove.Withdraw, DRAWER, false)))
    }

    @Test
    fun `изъять больше остатка нельзя, внести больше остатка можно`() {
        assertEquals(CashRefusal.NotEnough, refusal(CashRules.check("4275,51", CashMove.Withdraw, DRAWER, open)))
        assertTrue(CashRules.check("4275,50", CashMove.Withdraw, DRAWER, open) is CashDecision.Ready)
        assertTrue(CashRules.check("999999", CashMove.Deposit, DRAWER, open) is CashDecision.Ready)
    }

    @Test
    fun `без остатка изъятие проверит узел`() {
        assertTrue(CashRules.check("100", CashMove.Withdraw, null, open) is CashDecision.Ready)
    }

    @Test
    fun `остаток после операции считается в тиынах`() {
        assertEquals(437_550L, CashRules.after(DRAWER, BigDecimal("100"), CashMove.Deposit))
        assertEquals(417_550L, CashRules.after(DRAWER, BigDecimal("100"), CashMove.Withdraw))
        assertEquals(0L, CashRules.after(DRAWER, BigDecimal("4275.50"), CashMove.Withdraw))
        assertNull(CashRules.after(null, BigDecimal("100"), CashMove.Deposit))
    }

    @Test
    fun `повтор той же суммы идёт с прежним ключом`() {
        val first = CashRules.attemptFor(null, CashMove.Withdraw, BigDecimal("100")) { "key-1" }
        val again = CashRules.attemptFor(first, CashMove.Withdraw, BigDecimal("100.00")) { "key-2" }

        assertSame(first, again)
        assertEquals("key-1", again.key)
    }

    @Test
    fun `другая сумма или другое движение — другая операция`() {
        val first = CashAttempt(CashMove.Withdraw, BigDecimal("100"), "key-1")

        assertEquals("key-2", CashRules.attemptFor(first, CashMove.Withdraw, BigDecimal("200")) { "key-2" }.key)
        assertEquals("key-3", CashRules.attemptFor(first, CashMove.Deposit, BigDecimal("100")) { "key-3" }.key)
    }

    /** Остаток ящика тестового узла: 4275,50 ₸. */
    private companion object {
        const val DRAWER = 427_550L
    }
}
