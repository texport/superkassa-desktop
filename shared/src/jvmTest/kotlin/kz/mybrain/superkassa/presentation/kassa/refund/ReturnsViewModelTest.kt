package kz.mybrain.superkassa.presentation.kassa.refund

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScene
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Возврат без окна: основание, отмеченные строки и ключ повтора.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReturnsViewModelTest {
    private val notices = Notices()
    private val sent = mutableListOf<CreateReceiptCommand>()
    private val basis = ReturnsScene.sale(42, 90_000)

    /** Скидка 100 ₸ на весь проданный чек: строки 500 + 300 + 200 при итоге 900. */
    private val items = listOf(
        ReturnsScene.item("Баранина", "500.00", 1_000, "500.00"),
        ReturnsScene.item("Коньяк", "300.00", 1_000, "300.00"),
        ReturnsScene.item("Хлеб", "200.00", 1_000, "200.00")
    )

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(failing: Int = 0): ReturnsViewModel {
        var left = failing
        val core = ReturnsScene.core(listOf(basis), items, drawer = 1_000_000).apply {
            on("createReceipt") { args ->
                sent += args.single() as CreateReceiptCommand
                if (left-- > 0) throw IOException("timeout")
                ReceiptResponse("ret-${sent.size}", deliveryStatus = DeliveryStatus.ONLINE_OK)
            }
        }
        return returnsModel(CoreScene.services(core, SaleScene.signedIn(), notices), KassaPorts(FixedDeliverySetup()))
    }

    @Test
    fun `все строки чека со скидкой возвращают ровно его итог`() {
        val model = model()
        model.choose(basis)
        (0..2).forEach(model.refund::toggle)

        model.refund.refund()

        model.refund.confirm()

        val command = sent.single()
        val refund = command.payments.sumOf { Tenge.of(it.sum) }
        assertEquals(90_000L, refund, "возврат по отметкам больше, чем заплатил покупатель")
        assertEquals(listOf("Баранина", "Коньяк", "Хлеб"), command.items.map { it.name })
        assertEquals("SELL_RETURN", command.operation)
        assertIs<Message.Done>(notices.last)
        assertNull(model.state.value.refund, "выбор остался после возврата и приглашает вернуть ещё раз")
    }

    @Test
    fun `повтор того же возврата — тем же ключом, поправленная сумма — своим`() {
        val model = model(failing = 2)
        model.choose(basis)

        model.refund.refund()

        model.refund.confirm()
        assertIs<Message.NoAnswer>(notices.last)
        model.refund.refund()
        model.refund.confirm()
        model.refund.enter("100")
        model.refund.refund()
        model.refund.confirm()

        assertEquals(3, sent.size)
        assertEquals(sent[0].idempotencyKey, sent[1].idempotencyKey, "повтор того же возврата ушёл с новым ключом")
        assertNotEquals(sent[1].idempotencyKey, sent[2].idempotencyKey, "другая сумма ушла прежним ключом")
    }

    @Test
    fun `день и строки основания прочитаны, остаток ящика известен`() {
        val model = model()
        model.choose(basis)

        val state = model.state.value
        assertTrue(state.dayRead)
        assertEquals(listOf("sale-42"), state.candidates.map { it.id })
        assertEquals(3, state.refund?.items?.size)
        assertEquals(1_000_000L, state.cashInDrawer)
    }

    /** Возврат, как и деньги из ящика, уходит только после вопроса с суммой. */
    @Test
    fun `возврат спрашивает подтверждение, отмена ничего не отправляет`() {
        val model = model()
        model.choose(basis)

        model.refund.refund()
        assertTrue(model.state.value.confirming, "возврат не спросил подтверждения")
        assertTrue(sent.isEmpty(), "возврат ушёл без подтверждения")
        model.refund.cancel()

        assertFalse(model.state.value.confirming)
        assertTrue(sent.isEmpty(), "отменённый возврат ушёл в кассу")
        assertEquals(basis.id, model.state.value.refund?.basis?.id, "отмена сбросила выбранный чек")
    }

    /** Итог возврата — «Возврат продажи на 900,00 ₸: …», а не «оформлен, состояние 900,00 ₸». */
    @Test
    fun `итог возврата называет сумму`() {
        val model = model()
        model.choose(basis)

        model.refund.refund()
        model.refund.confirm()

        val said = (notices.last as Message.Done).text
        assertTrue(said.startsWith("Возврат продажи на 900,00"), said)
    }
}
