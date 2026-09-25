package kz.mybrain.superkassa.presentation.kassa.refund

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaExtremes.Case
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.KassaWindow
import kz.mybrain.superkassa.WindowSteps
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.eachWindow
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.ReturnsScene
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.navigation.step.ReturnBasisKey
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.wholeOnScreen
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Возврат по чеку из пятидесяти строк в любом окне кассы.
 *
 * Сумма возврата и «Вернуть покупателю» лежали под составом чека: у чека
 * из пятидесяти строк поля суммы на экране не было, а кнопка оставалась
 * нажимаемой. Меряется, видны ли целиком кнопка и поле суммы после выбора
 * основания и не обрезаны ли номер и признак в строке списка.
 *
 * Кадры — `/tmp/adaptive-kassa-returns-*.png`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReturnsAdaptiveTest {

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun check(probe: KassaProbe, case: Case): List<String> {
        val desk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))
        val bases = (1L..BASES).map { KassaExtremes.sale(it) }
        val sold = KassaExtremes.sold(ITEMS).map {
            ReturnsScene.item(it.name, it.price, it.quantityThousandths, it.sum)
        }
        val services = CoreScene.services(ReturnsScene.core(bases, sold), SaleScene.signedIn())
        val model = returnsModel(services, KassaPorts(FixedDeliverySetup()))
        val texts = textsOf(case.language).common
        val journal = textsOf(case.language).journal.returns
        val failures = mutableListOf<String>()
        probe.show(case.look, case.language) {
            KassaWindow(desk, Section.Returns) {
                WindowSteps { step, _ -> ReturnsScreen(model, step is ReturnBasisKey) }
            }
        }
        probe.frame(KassaProbe.SETTLE)
        val sign = probe.part("${journal.fiscalSign}: ")
        val number = probe.part("${texts.returns.receiptNo} 1")
        if (sign == null || probe.cut(sign)) failures += "${case.tag}: признак в строке обрезан"
        if (number == null || probe.cut(number)) failures += "${case.tag}: номер в строке обрезан"
        probe.save("returns-list-${case.tag}")
        probe.click("${texts.returns.receiptNo} 1")
        probe.save("returns-chosen-${case.tag}")
        val button = probe.node(texts.returns.giveBack)
        val amount = probe.node(journal.amount)
        println("возврат ${case.tag}: кнопка ${button?.boundsInRoot}, сумма ${amount?.boundsInRoot}")
        if (button?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: кнопки не видно"
        if ((button?.size?.height ?: 0) < TOUCH) failures += "${case.tag}: кнопка ниже цели нажатия"
        if (amount?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: суммы не видно"
        return failures
    }

    @Test
    fun `кнопка и сумма возврата видны при чеке из пятидесяти строк`() {
        val failures = eachWindow { probe, case -> check(probe, case) }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val BASES = 30L
        const val ITEMS = 50
        const val TOUCH = 48
    }
}
