package kz.mybrain.superkassa

import kz.mybrain.superkassa.KassaExtremes.Case
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.presentation.Section
import kz.mybrain.superkassa.presentation.cash.CashScreen
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.strings.stringsOf
import kz.mybrain.superkassa.presentation.theme.ContentWidths
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Деньги: остаток в миллиарды и десять движений за сутки.
 *
 * Меряется, что «Внести» видна и не ниже цели нажатия, остаток ящика
 * не срезан, а сумма движения стоит не дальше читаемой ширины от его
 * названия — на широком окне она уезжала на полторы тысячи точек.
 *
 * Кадры — `/tmp/adaptive-kassa-cash-*.png`.
 */
class CashAdaptiveTest {

    private fun move(no: Long) = Document(
        id = "cash-$no",
        docNo = no,
        docType = if (no % 2 == 0L) "CASH_IN" else "CASH_OUT",
        ofdStatus = "SENT",
        totalAmount = DRAWER - no,
        createdAt = System.currentTimeMillis()
    )

    private fun check(probe: KassaProbe, case: Case): List<String> {
        val session = KassaScene.session(
            "cash-${case.tag}",
            shift = KassaScene.openShift(),
            cashInDrawerTiyn = DRAWER,
            journal = (1L..MOVES).map { move(it) }
        )
        session.switchLanguage(case.language)
        val texts = stringsOf(case.language)
        val failures = mutableListOf<String>()
        probe.show(case.look, case.language) {
            KassaWindow(session, Section.Cash) { CashScreen(session) }
        }
        probe.save("cash-${case.tag}")
        val deposit = probe.node(texts.cash.deposit)
        val drawer = probe.part(Money.formatTiyn(DRAWER))
        val amount = probe.part(Money.formatTiyn(DRAWER - 1))?.boundsInRoot
        val deposited = probe.parts().firstOrNull { it.label() == texts.cash.deposit }
        println("деньги ${case.tag}: кнопка ${deposit?.size}, ящик ${drawer?.boundsInRoot}, движение $amount")
        if (deposit?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: «Внести» не видна"
        if ((deposit?.size?.height ?: 0) < TOUCH) failures += "${case.tag}: «Внести» ниже цели нажатия"
        if (drawer?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: остаток срезан"
        val reach = amount?.right?.minus(deposited?.boundsInRoot?.left ?: 0f) ?: 0f
        val limit = ContentWidths.reading.value * case.scale.factor + SLACK
        if (reach > limit) failures += "${case.tag}: сумма в $reach от края"
        return failures
    }

    @Test
    fun `кнопка и остаток видны, сумма движения рядом с названием`() {
        val failures = eachWindow { probe, case -> check(probe, case) }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val DRAWER = 123_456_789_012L
        const val MOVES = 10L
        const val TOUCH = 48
        const val SLACK = 1f
    }
}
