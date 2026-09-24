package kz.mybrain.superkassa.presentation.kassa.cash

import kotlinx.coroutines.Dispatchers
import kz.kazakhtelecom.proto.v203.MoneyPlacementEnum
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.model.cash.CashDecision
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LosingKassa
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.kassa.tiyn
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Деньги на настоящем ядре: внесение, изъятие, остаток ящика и отказ
 * при нехватке. Итог сверяется со счётчиком ядра и с тем, что получил БФД.
 */
class CashCoreTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    /** Кассир набирает сумму, выбирает движение и подтверждает его. */
    private fun CashViewModel.move(amount: String, move: CashMove) {
        enter(amount)
        ask(move)
        confirm()
    }

    @Test
    fun `внесение и изъятие — остаток ящика как у ядра, в БФД оба движения`() {
        desk.seated()
        val model = cashModel(desk.app).also { it.visit() }

        desk.bench.clock.move(-2 * SECOND)
        model.move("5000", CashMove.Deposit)
        desk.bench.clock.move(SECOND)
        model.move("1200,50", CashMove.Withdraw)

        assertIs<Message.Done>(desk.said, desk.saidText)
        assertEquals(LEFT, model.state.value.cashInDrawer)
        val placements = desk.bfd.moneyPlacements().map { it.operation to it.sum.tiyn() }
        val expected = listOf(
            MoneyPlacementEnum.MONEY_PLACEMENT_DEPOSIT to FIVE_THOUSAND,
            MoneyPlacementEnum.MONEY_PLACEMENT_WITHDRAWAL to WITHDRAWN
        )
        assertEquals(expected, placements)
        assertEquals(listOf("CASH_OUT", "CASH_IN"), model.state.value.recent.map { it.docType })
        assertEquals("", model.state.value.amount, "проведённая сумма осталась в поле")
    }

    @Test
    fun `изъять больше, чем в ящике, экран не даёт — и в БФД ничего не уходит`() {
        desk.seated()
        val model = cashModel(desk.app).also { it.visit() }
        model.enter("100")

        model.ask(CashMove.Withdraw)

        assertTrue(model.state.value.decision(CashMove.Withdraw) !is CashDecision.Ready)
        assertNull(model.state.value.asked, "подтверждение изъятия из пустого ящика спрошено")
        assertTrue(desk.bfd.moneyPlacements().isEmpty())
    }

    @Test
    fun `ящик опустел за спиной экрана — касса отказывает своими словами, сумма остаётся`() {
        val kassa = desk.seated().also { it.cashIn("5000.00") }
        val model = cashModel(desk.app).also { it.visit() }
        kassa.cashOut("5000.00")

        model.move("1000", CashMove.Withdraw)

        val refusal = assertIs<Message.Refusal>(desk.said, desk.saidText)
        assertTrue(refusal.text.isNotBlank())
        assertEquals("1000", model.state.value.amount, "сумма пропала после отказа")
        assertEquals(0L, model.state.value.cashInDrawer, "остаток не перечитан после отказа")
        assertEquals(2, desk.bfd.moneyPlacements().size)
    }

    @Test
    fun `ответ на внесение потерян — повтор тем же ключом не вносит второй раз`() {
        val kassa = desk.seated()
        val losing = LosingKassa(EmbeddedKassa(desk.bench.api, Dispatchers.Unconfined))
        val model = cashModel(CoreScene.app(losing, desk.signIn, desk.notices)).also { it.visit() }

        model.move("5000", CashMove.Deposit)
        assertIs<Message.NoAnswer>(desk.said, desk.saidText)
        model.ask(CashMove.Deposit)
        model.confirm()

        assertIs<Message.Done>(desk.said, desk.saidText)
        assertEquals(1, desk.bfd.moneyPlacements().size, "внесение ушло в БФД дважды")
        val drawer = kassa.api.listCounters(kassa.kkmId, kassa.adminPin)
            .single { it.scope == "GLOBAL" && it.key == "cash.sum" }
        assertEquals(FIVE_THOUSAND, drawer.value)
    }

    private companion object {
        const val FIVE_THOUSAND = 500_000L
        const val WITHDRAWN = 120_050L
        const val LEFT = FIVE_THOUSAND - WITHDRAWN
        const val SECOND = 1_000L
    }
}
