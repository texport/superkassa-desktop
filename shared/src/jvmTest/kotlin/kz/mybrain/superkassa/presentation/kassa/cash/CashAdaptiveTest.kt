package kz.mybrain.superkassa.presentation.kassa.cash

import kz.mybrain.superkassa.KassaExtremes.Case
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.KassaWindow
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.eachWindow
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.theme.size.CardGrid
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.wholeOnScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Деньги: остаток в миллиарды и десять движений за сутки.
 *
 * Меряется, что «Внести» видна и не ниже цели нажатия, остаток ящика
 * не срезан, а сумма движения стоит в карточке движений, не дальше
 * ширины её столбца от заголовка.
 * Движения на широком окне стоят столбцом рядом с формой, а не под ней.
 *
 * Кадры — `/tmp/adaptive-kassa-cash-*.png`.
 */
class CashAdaptiveTest {

    private fun move(no: Long) =
        CoreScene.document(
            "cash-$no",
            type = if (no % 2 == 0L) "CASH_IN" else "CASH_OUT",
            amount = DRAWER - no,
            status = "SENT"
        )
            .copy(docNo = no, createdAt = System.currentTimeMillis())

    /** Ящик в миллиарды и десять движений за сутки. */
    private val drawer = CashUiState(
        kkm = CoreScene.kkm(),
        signedIn = true,
        shiftOpen = true,
        cashInDrawer = DRAWER,
        recent = (1L..MOVES).map { move(it) },
        recentRead = true,
        recentLoading = false
    )

    private fun check(probe: KassaProbe, case: Case): List<String> {
        val desk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))
        val texts = textsOf(case.language).common
        val failures = mutableListOf<String>()
        probe.show(case.look, case.language) {
            KassaWindow(desk, Section.Cash) { CashContent(drawer) }
        }
        probe.save("cash-${case.tag}")
        val deposit = probe.node(texts.cash.deposit)
        val drawer = probe.part(Money.formatTiyn(DRAWER))
        val amount = probe.part(Money.formatTiyn(DRAWER - 1))?.boundsInRoot
        // Движения — своей карточкой: сумма меряется от её левого края, от заголовка.
        val recent = probe.part(textsOf(case.language).kassa.money.drawer.recent)
        println("деньги ${case.tag}: кнопка ${deposit?.size}, ящик ${drawer?.boundsInRoot}, движение $amount")
        if (deposit?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: «Внести» не видна"
        if ((deposit?.size?.height ?: 0) < TOUCH) failures += "${case.tag}: «Внести» ниже цели нажатия"
        if (drawer?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: остаток срезан"
        val reach = amount?.right?.minus(recent?.boundsInRoot?.left ?: 0f) ?: 0f
        // Сумма стоит в карточке движений: её столбец — не шире двух наименьших,
        // а на широком окне — половина окна, где карточки стоят рядом.
        val column = maxOf(CardGrid.columnMin.value * 2 + CardGrid.gap.value, case.width / 2f)
        val limit = column * case.scale.factor + SLACK
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
