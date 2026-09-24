package kz.mybrain.superkassa.presentation.shift.dashboard

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaExtremes.Case
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.KassaWindow
import kz.mybrain.superkassa.eachWindow
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.kassa.state
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.wholeOnScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Главный экран со сменой, где БФД отверг тридцать документов.
 *
 * Пачка отказов стояла над списком смены и вытесняла его за нижний край.
 * Меряется, что строка документа смены видна целиком, «Закрыть смену»
 * видна и не ниже цели нажатия, остаток ящика в миллиарды не срезан,
 * а перечень отказов прокручивается сам.
 *
 * Кадры — `/tmp/adaptive-kassa-dashboard-*.png`.
 */
class DashboardAdaptiveTest {

    private fun check(probe: KassaProbe, case: Case): List<String> {
        val refused = (1L..REFUSED).map { KassaExtremes.sale(it, refused = true) }
        val accepted = (REFUSED + 1..REFUSED + ACCEPTED).map { KassaExtremes.sale(it) }
        val desk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))
        val state = DashboardScene.state(shift = CoreScene.openShift(), documents = refused + accepted, cash = DRAWER)
        val texts = textsOf(case.language).common
        val failures = mutableListOf<String>()
        probe.show(case.look, case.language) {
            KassaWindow(desk, Section.Dashboard) { DashboardContent(state) }
        }
        probe.save("dashboard-${case.tag}")
        val close = probe.node(texts.dashboard.closeShift)
        val drawer = probe.part(Money.formatTiyn(DRAWER))
        val document = probe.part("${texts.dashboard.documentNo} 1")
        // Перечень отказов — сразу под заголовком карточки: пояснение
        // к отказу в тесном окне бывает за краем видимой части.
        val heading = probe.part("${texts.dashboard.refused}: $REFUSED")?.boundsInRoot
        println("главная ${case.tag}: кнопка ${close?.size}, ящик ${drawer?.boundsInRoot}")
        if (close?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: кнопки не видно"
        if ((close?.size?.height ?: 0) < TOUCH) failures += "${case.tag}: кнопка ниже цели нажатия"
        if (drawer?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: остаток срезан"
        if (document?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: документов не видно"
        // В самом тесном окне на крупнейшем шрифте списку смены отдана
        // его доля первой, и от карточки отказов остаётся заголовок
        // с их числом: прокручивать там нечего, и это не помеха —
        // каждый отклонённый документ помечен и в списке смены.
        val room = probe.part(REFUSAL_WORDS)?.boundsInRoot?.height ?: 0f
        if (heading == null) {
            failures += "${case.tag}: перечня отказов не видно"
        } else if (room > 0f) {
            val before = probe.frame()
            probe.wheel(Offset(heading.center.x, heading.bottom + BELOW_HEADING), WHEEL)
            if (probe.frame().contentEquals(before)) failures += "${case.tag}: отказы не прокручиваются"
        }
        return failures
    }

    @Test
    fun `документы смены, кнопка и остаток видны при тридцати отказах`() {
        val failures = eachWindow { probe, case -> check(probe, case) }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val REFUSED = 30L
        const val ACCEPTED = 20L
        const val DRAWER = 123_456_789_012L
        const val TOUCH = 48
        const val WHEEL = 6f

        /** Пояснение БФД к отказу: видно оно — виден и перечень. */
        const val REFUSAL_WORDS = "Same customer"

        /** Первая строка перечня стоит ниже заголовка карточки на столько. */
        const val BELOW_HEADING = 24f
    }
}
