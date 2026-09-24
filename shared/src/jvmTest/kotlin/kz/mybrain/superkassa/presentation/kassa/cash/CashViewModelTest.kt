package kz.mybrain.superkassa.presentation.kassa.cash

import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsScene
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScene
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Деньги без окна: остаток, движения за сутки, внесение и изъятие.
 *
 * Ключ попытки живёт до проведённых денег: та же сумма после неизвестного
 * исхода уходит с тем же ключом, и касса не проводит её дважды.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CashViewModelTest {
    private val notices = Notices()
    private val keys = mutableListOf<String>()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun core(documents: Int = 0, failing: Int = 0): FakeCore {
        val day = (1L..documents).map {
            CoreScene.document(
                "cash-$it",
                type = "CASH_IN",
                amount = 10_000,
                status = "SENT"
            )
        }
        var left = failing
        return ReturnsScene.core(day).apply {
            on("cashIn") { args ->
                keys += (args[2] as CashOperationRequest).idempotencyKey
                if (left-- > 0) throw IOException("timeout")
                CashOperationResponse("doc-${keys.size}", DeliveryStatus.ONLINE_OK)
            }
        }
    }

    private fun model(core: FakeCore) = cashModel(CoreScene.app(core, SaleScene.signedIn(), notices))

    @Test
    fun `остаток и движения за сутки прочитаны целиком, а не первой страницей`() {
        val model = model(core(documents = 3))

        val state = model.state.value
        assertEquals(125_000L, state.cashInDrawer)
        assertTrue(state.recentRead)
        assertEquals(3, state.recent.size)
        assertTrue(state.shiftOpen)
    }

    @Test
    fun `повтор той же суммы после сбоя идёт с тем же ключом`() {
        val model = model(core(failing = 1))
        model.enter("1000")

        model.ask(CashMove.Deposit)
        model.confirm()
        assertIs<Message.NoAnswer>(notices.last)
        model.ask(CashMove.Deposit)
        model.confirm()

        assertEquals(2, keys.size)
        assertEquals(1, keys.distinct().size, "повтор ушёл с новым ключом")
        assertEquals("", model.state.value.amount, "проведённая сумма осталась в поле")
        assertNull(model.state.value.attempt)
    }

    @Test
    fun `отказ кассы назван её словами, и окно не висит`() {
        val core = core().apply { refuse("cashIn", "SHIFT_NOT_OPEN", ru = "Смена закрыта") }
        val model = model(core)
        model.enter("1000")

        model.ask(CashMove.Deposit)
        model.confirm()

        assertEquals(Message.Refusal("Смена закрыта", "SHIFT_NOT_OPEN"), notices.last)
        assertFalse(model.state.value.busy)
        assertNull(model.state.value.asked)
    }

    @Test
    fun `молчание кассы о сутках не выдаётся за сутки без движений`() {
        val core = core().apply { on("listFiscalDocumentsByPeriod") { error("disk") } }
        val model = model(core)

        assertFalse(model.state.value.recentRead)
        assertIs<Message.Failed>(notices.last)
    }
}
