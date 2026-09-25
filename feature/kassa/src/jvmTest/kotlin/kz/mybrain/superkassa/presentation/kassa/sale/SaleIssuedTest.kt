package kz.mybrain.superkassa.presentation.kassa.sale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.kassa.SaleScene.receipts
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Notices
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Итог пробитого чека и ручной ввод следующего.
 *
 * После чека кассир видит сумму и сдачу и может показать или распечатать
 * чек; следующий чек начинается с раскрытого ручного ввода — подсказка
 * пустого чека зовёт ввести позицию вручную, и поля обязаны быть на месте.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SaleIssuedTest {
    private val memory = MemoryWorkplace()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(): SaleViewModel {
        val core = SaleScene.core().also { it.receipts() }
        val services = CoreScene.services(core, SaleScene.signedIn(), Notices(), memory)
        return saleModel(services, KassaPorts(FixedDeliverySetup())).also { it.visit() }
    }

    private fun SaleViewModel.bread() {
        entry.editDraft(PositionDraft(name = "Хлеб «Тандыр»", price = "450", measureUnitCode = "796"))
        assertTrue(entry.addDraft(), "позиция не встала в чек")
    }

    @Test
    fun `пробитый чек остаётся итогом с суммой и сдачей, пока не начат следующий`() {
        val model = model()
        model.bread()
        model.form.taken("1000")

        model.issue()

        val issued = assertNotNull(model.state.value.shownIssued, "итога пробитого чека нет")
        assertEquals(45_000L, issued.total)
        assertEquals(55_000L, issued.change)
        assertTrue(issued.documentId.isNotBlank())
        model.bread()
        assertNull(model.state.value.shownIssued, "итог прежнего чека стоит над новым")
    }

    @Test
    fun `следующий чек убирает итог`() {
        val model = model()
        model.bread()
        model.issue()

        model.nextReceipt()

        assertNull(model.state.value.shownIssued)
    }

    @Test
    fun `новый чек начинается с раскрытого ручного ввода, даже если его свернули`() {
        val model = model()
        model.togglePanel(SalePanel.PositionEntry)
        assertFalse(model.state.value.expanded(SalePanel.PositionEntry))
        model.bread()

        model.issue()

        assertTrue(model.state.value.expanded(SalePanel.PositionEntry), "пустой чек со свёрнутым ручным вводом")
        model.visit()
        assertTrue(model.state.value.expanded(SalePanel.PositionEntry), "вход в раздел свернул ручной ввод")
    }

    @Test
    fun `код с буквами — маркировочный — набирается целиком`() {
        val model = model()

        model.entry.typeBarcode(" 0104600000000000 21aBc ")

        assertEquals("010460000000000021aBc", model.state.value.search.barcode)
    }

    /** После позиции и после чека следующий скан идёт в штрихкод, а не в «Принято». */
    @Test
    fun `позиция и пробитый чек возвращают фокус в штрихкод`() {
        val model = model()
        val start = model.state.value.barcodeTurn

        model.bread()
        val added = model.state.value.barcodeTurn
        model.issue()

        assertTrue(added > start, "позиция не вернула фокус в штрихкод")
        assertTrue(model.state.value.barcodeTurn > added, "чек не вернул фокус в штрихкод")
    }
}
