package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.ReturnsScene
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.kassa.SaleScene.receipts
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.ProvideWindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowModels
import kz.mybrain.superkassa.presentation.kassa.cash.CashScreen
import kz.mybrain.superkassa.presentation.kassa.cash.cashViewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.section.Section
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Чек переживает уход в другой раздел — в окне, а не только в модели.
 *
 * Прежде корзина и ключ попытки жили в памяти экрана: кассир, ушедший
 * в «Деньги» за разменом, возвращался к пустому чеку, а после ответа
 * «не дождались» набирал его заново уже с новым ключом — и касса
 * пробивала второй чек.
 */
class SaleSectionTest {
    private val notices = Notices()

    private var section by mutableStateOf(Section.Sale)
    private val seen = mutableListOf<SaleViewModel>()
    private var cashShown = 0

    @Test
    fun `пробили, ответа нет, ушли в деньги, вернулись, повторили — в кассе один документ`(): Unit = inlineMain {
        val core = ReturnsScene.core(emptyList())
        val receipts = core.receipts()
        receipts.failing += IOException("timeout")
        val app = CoreScene.app(core, SaleScene.signedIn(), notices)
        val models = WindowModels()

        RenderProbe { Window(app, models) }.use { probe ->
            settle(probe)
            val key = issueWithoutAnswer(seen.last())
            walkToCashAndBack(probe)
            val back = seen.last()
            assertEquals(1, seen.distinct().size, "продажа после возвращения — другая модель")
            assertEquals(1, back.state.value.basket.positions.size, "чек потерян при смене раздела")
            assertEquals(key, back.state.value.attemptKey, "ключ попытки сменился при смене раздела")
            back.issue()
        }
        models.close()

        assertEquals(1, receipts.commands.map { it.idempotencyKey }.distinct().size)
        assertEquals(1, receipts.documents.size, "касса пробила второй документ")
    }

    /** Окно с моделями окна: раздел меняется, модели остаются. */
    @Composable
    private fun Window(app: AppContainer, models: WindowModels) {
        ProvideWindowModels(models) {
            when (section) {
                Section.Sale -> SaleScreen(saleViewModel(app.services, app.areas.kassa).also { seen += it })
                else -> CashScreen(cashViewModel(app.services)).also { cashShown++ }
            }
        }
    }

    /** Хлеб в чек и «Пробить чек», а касса не ответила; ключ этой попытки. */
    private fun issueWithoutAnswer(sale: SaleViewModel): String {
        sale.entry.editDraft(PositionDraft(name = "Хлеб «Тандыр»", price = "450", measureUnitCode = "796"))
        sale.entry.addDraft()
        sale.issue()
        assertIs<Message.NoAnswer>(notices.last)
        return sale.state.value.attemptKey
    }

    /** Кассир ушёл в «Деньги» за разменом и вернулся. */
    private fun walkToCashAndBack(probe: RenderProbe) {
        val before = seen.size
        section = Section.Cash
        settle(probe)
        assertTrue(cashShown > 0, "раздел «Деньги» так и не открылся")
        section = Section.Sale
        settle(probe)
        assertTrue(seen.size > before, "продажа не вернулась на экран")
    }

    private fun settle(probe: RenderProbe) {
        Snapshot.sendApplyNotifications()
        repeat(SETTLE) { probe.frame() }
    }

    private companion object {
        const val SETTLE = 10
    }
}
