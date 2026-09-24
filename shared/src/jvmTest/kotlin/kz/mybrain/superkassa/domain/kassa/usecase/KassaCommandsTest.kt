package kz.mybrain.superkassa.domain.kassa.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationRequest
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.cash.CashAttempt
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundPlan
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Фискальные команды кассы — без экрана.
 *
 * Сценарий сам собирает запрос из попытки кассира и сам перечитывает кассу
 * после команды: модель экрана не видит ни пина, ни запроса.
 */
class KassaCommandsTest {

    private val signed = SignIn().apply { enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN) }

    @Test
    fun `деньги уходят суммой в тенге и ключом попытки, касса перечитывается`(): Unit = runBlocking {
        var sent: CashOperationRequest? = null
        val core = FakeCore().apply {
            on("cashOut") { args ->
                sent = args[2] as CashOperationRequest
                error("no answer")
            }
            on("getKkm") { CoreScene.kkm().copy(isShiftOpen = true) }
        }

        val answer = MoveCash(core.kassa(), signed)(CashAttempt(CashMove.Withdraw, 150_050L, "key-1"))

        assertIs<Answer.Failed>(answer)
        assertEquals(Tenge.decimal(150_050L), sent?.amount)
        assertEquals("key-1", sent?.idempotencyKey)
        assertTrue(signed.state.value.kkm?.isShiftOpen == true, "касса после команды не перечитана")
    }

    @Test
    fun `возврат по основанию без номера до кассы не доходит`(): Unit = runBlocking {
        val core = FakeCore()
        val plan = RefundPlan(
            kind = ReturnKind.Sell,
            basis = CoreScene.document("d-1"),
            kgdKkmId = "000000200042",
            refundTiyn = 150_000L,
            lines = emptyList(),
            lineName = "Возврат",
            payments = emptyList(),
            domain = DomainKind.Trading.plain
        )

        assertIs<Answer.Failed>(IssueRefund(core.kassa(), signed)(plan, "key-1"))
        assertTrue(core.calls.isEmpty(), "касса получила возврат без основания: ${core.calls}")
    }

    @Test
    fun `без кассира команда не уходит`(): Unit = runBlocking {
        val core = FakeCore().apply { on("getKkm") { CoreScene.kkm() } }

        val answer = MoveCash(core.kassa(), SignIn())(CashAttempt(CashMove.Deposit, 100L, "key-1"))

        assertIs<Answer.Failed>(answer)
        assertTrue("cashIn" !in core.calls)
    }
}
