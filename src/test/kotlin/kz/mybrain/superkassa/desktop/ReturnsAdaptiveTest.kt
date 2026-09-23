package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.KassaExtremes.Case
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.returns.ReturnsScreen
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
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
class ReturnsAdaptiveTest {

    private fun check(probe: KassaProbe, case: Case): List<String> {
        val session = KassaScene.session(
            "returns-${case.tag}",
            shift = KassaScene.openShift(),
            journal = (1L..BASES).map { KassaExtremes.sale(it) },
            sold = KassaExtremes.sold(ITEMS)
        )
        session.switchLanguage(case.language)
        val texts = stringsOf(case.language)
        val journal = journalTexts(case.language).returns
        val failures = mutableListOf<String>()
        probe.show(case.look, case.language) {
            KassaWindow(session, Section.Returns) { ReturnsScreen(session) }
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
