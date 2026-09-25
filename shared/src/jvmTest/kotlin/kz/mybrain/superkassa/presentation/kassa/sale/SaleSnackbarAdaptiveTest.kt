package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaExtremes.Case
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.KassaWindow
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.eachWindow
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.MessageEffect
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Отказ кассы снекбаром не ложится на кассовую колонку.
 *
 * Кадры — `/tmp/adaptive-kassa-sale-snackbar-*.png`.
 */
class SaleSnackbarAdaptiveTest {

    /**
     * Отказ кассы снекбаром не ложится на кассу.
     *
     * Снекбар посередине окна закрывал «Пробить чек» и «Принято»: кассир
     * не мог нажать кнопку, пока читал, почему нажатие не прошло.
     * Меряется пересечение прямоугольников на всех окнах, где касса стоит
     * рядом с чеком.
     */
    @Test
    fun `снекбар отказа не закрывает кнопку и принятые деньги`() {
        val failures = eachWindow(KassaExtremes.CASES.filter { it.width >= DESKTOP }) { probe, case ->
            val desk = SaleScene.window()
            val refusal = Message.Refusal(REFUSAL, "SHIFT_LONGER_THAN_DAY")
            probe.show(case.look, case.language) {
                val host = remember { SnackbarHostState() }
                KassaWindow(desk, Section.Sale, host) {
                    MessageEffect(refusal, host) {}
                    SaleContent(SaleScene.taxi(KassaExtremes.basket(FIFTY), SaleForm()))
                }
            }
            probe.save("sale-snackbar-${case.tag}")
            covered(probe, case)
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /** Что снекбар закрыл собой: кнопку, «Принято» — или его нет вовсе. */
    private fun covered(probe: KassaProbe, case: Case): List<String> {
        val texts = textsOf(case.language).common
        val bar = probe.part(REFUSAL_PROBE)?.boundsInRoot
        val issue = probe.node(texts.receipt.issueSale)?.boundsInRoot
        val taken = probe.node(texts.receipt.taken)?.boundsInRoot
        println("снекбар ${case.tag}: $bar, кнопка $issue, принято $taken")
        return listOfNotNull(
            "${case.tag}: снекбара нет".takeIf { bar == null },
            "${case.tag}: закрыта кнопка".takeIf { bar != null && issue != null && bar.overlaps(issue) },
            "${case.tag}: закрыто «Принято»".takeIf { bar != null && taken != null && bar.overlaps(taken) }
        )
    }

    private companion object {
        const val FIFTY = 50

        /** По этим словам снекбар отличается от плашек шапки. */
        const val REFUSAL_PROBE = "Z-отчётом"

        /** Окна, где касса стоит рядом с чеком: настольные. */
        const val DESKTOP = 960

        /** Отказ кассы длиной в несколько строк. */
        const val REFUSAL = "Смена открыта больше суток: касса не принимает фискальные документы, пока смена " +
            "не будет закрыта Z-отчётом. Закройте смену на главном экране и откройте новую, затем повторите чек"
    }
}
