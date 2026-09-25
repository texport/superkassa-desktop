package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.kassa.SaleScene.receipts
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Продажа без окна: чек, его ключ повтора и итог словами кассира.
 *
 * Ключ попытки живёт в модели, а не в экране: ответ потерялся, кассир
 * ушёл за разменом и вернулся — повтор идёт с тем же ключом, и касса
 * не пробивает второй чек.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SaleViewModelTest {
    private val notices = Notices()
    private val texts = textsOf(Language.Ru).common

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(core: FakeCore) =
        saleModel(CoreScene.services(core, SaleScene.signedIn(), notices), KassaPorts(FixedDeliverySetup()))

    /** Позиция руками: хлеб за 450 ₸. */
    private fun SaleViewModel.bread() {
        entry.editDraft(PositionDraft(name = "Хлеб «Тандыр»", price = "450", measureUnitCode = "796"))
        assertTrue(entry.addDraft(), "позиция не встала в чек")
    }

    @Test
    fun `повтор после неизвестного исхода идёт с тем же ключом, и документ один`() {
        val core = SaleScene.core()
        val receipts = core.receipts()
        receipts.failing += IOException("timeout")
        val model = model(core)
        model.bread()

        model.issue()
        assertIs<Message.NoAnswer>(notices.last, "неизвестный исход назван отказом или молчанием")
        assertEquals(1, model.state.value.basket.positions.size, "чек забыт, хотя мог не пробиться")
        model.visit()
        model.issue()

        assertEquals(2, receipts.commands.size)
        assertEquals(1, receipts.commands.map { it.idempotencyKey }.distinct().size, "повтор ушёл с новым ключом")
        assertEquals(1, receipts.documents.size, "касса пробила второй документ")
        assertTrue(model.state.value.basket.positions.isEmpty(), "принятый чек остался в корзине")
    }

    @Test
    fun `принятый чек очищает корзину и даёт следующему свой ключ`() {
        val core = SaleScene.core()
        val receipts = core.receipts()
        val model = model(core)

        model.bread()
        model.issue()
        model.bread()
        model.issue()

        assertEquals(2, receipts.documents.size, "второй чек подряд касса сочла повтором первого")
        assertEquals(Message.Done("${texts.sale.sale}: ${texts.common.deliveredToOfd}"), notices.last)
    }

    @Test
    fun `нет связи — чек в автономной очереди, и это сказано`() {
        val core = SaleScene.core()
        core.receipts(DeliveryStatus.OFFLINE_QUEUED)
        val model = model(core)

        model.bread()
        model.issue()

        assertEquals(Message.Done("${texts.sale.sale}: ${texts.common.queuedNoLink}"), notices.last)
    }

    @Test
    fun `отказ кассы — её словами на языке кассира, чек и ключ остаются`() {
        val core = SaleScene.core()
        core.refuse(
            "createReceipt",
            "PAYMENTS_TOTAL_MISMATCH",
            ru = "Оплата не сходится с итогом",
            kk = "Төлем сай емес"
        )
        val model = model(core)
        model.bread()
        val key = model.state.value.attemptKey

        model.issue()

        assertEquals(Message.Refusal("Оплата не сходится с итогом", "PAYMENTS_TOTAL_MISMATCH"), notices.last)
        assertEquals(key, model.state.value.attemptKey)
        assertFalse(model.state.value.issuing, "кнопка погасла молча")
    }

    @Test
    fun `запертый пин назван с оставшимся временем`() {
        val core = SaleScene.core().apply { on("createReceipt") { throw PinLockedException(retryAfterSeconds = 240) } }
        val model = model(core)
        model.bread()

        model.issue()

        val refusal = assertIs<Message.Refusal>(notices.last)
        assertEquals("PIN_LOCKED", refusal.code)
        assertTrue(refusal.text.contains("4"), "не сказано, сколько ждать: ${refusal.text}")
    }

    @Test
    fun `чек не пробивается, пока смена закрыта, и касса о нём не спрошена`() {
        val core = SaleScene.core(shift = null)
        val receipts = core.receipts()
        val model = model(core)
        model.bread()

        model.issue()

        assertEquals(SaleBlock.ShiftClosed, model.state.value.block)
        assertTrue(receipts.commands.isEmpty())
    }

    @Test
    fun `другой кассир начинает с пустого чека и своего ключа`() {
        val signIn = SaleScene.signedIn()
        val model = saleModel(CoreScene.services(SaleScene.core(), signIn, notices), KassaPorts(FixedDeliverySetup()))
        model.bread()
        val key = model.state.value.attemptKey

        signIn.enter(CoreScene.kkm(), CoreScene.cashier(admin = false).copy(userId = "u-2"), "5678")

        assertTrue(model.state.value.basket.positions.isEmpty(), "чужой чек достался другому кассиру")
        assertNotEquals(key, model.state.value.attemptKey)
    }
}
