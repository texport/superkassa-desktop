package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaExtremes.Case
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.KassaWindow
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.TableColumns
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.eachWindow
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.label
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.wholeOnScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Продажа в любом окне, где работает касса.
 *
 * Меряется то, что кассир обязан видеть без прокрутки: «Пробить чек»,
 * поле штрихкода и причину под погашенной кнопкой. У строки чека —
 * ширина наименования: в окне по умолчанию оно сжималось до одной буквы.
 *
 * Кадры — `/tmp/adaptive-kassa-sale-*.png`.
 */
class SaleAdaptiveTest {

    private fun desk(): KassaDesk = SaleScene.window()

    private fun sale(basket: Basket, form: SaleForm) = SaleScene.taxi(basket, form)

    private class Measured(val issue: Boolean, val barcode: Boolean, val name: Int, val reason: Boolean)

    private fun measure(probe: KassaProbe, case: Case, basket: Basket, form: SaleForm, shot: String): Measured {
        val desk = desk()
        val texts = textsOf(case.language).common
        val sale = textsOf(case.language).kassa.sale
        probe.show(case.look, case.language) {
            KassaWindow(desk, Section.Sale) { SaleContent(sale(basket, form)) }
        }
        probe.save("sale-$shot-${case.tag}")
        val name = probe.parts().filter { it.label().startsWith(KassaExtremes.LONG_NAME.take(NAME_PROBE)) }
            .minOfOrNull { it.size.width } ?: -1
        // Причина под погашенной кнопкой: у пустого чека — пустой чек,
        // у набранного — незаполненный реквизит такси.
        val reason = probe.part(sale.blockEmptyBasket) ?: probe.part(sale.carNumber.take(REASON_PROBE))
        return Measured(
            issue = probe.node(texts.receipt.issueSale)
                ?.let { it.wholeOnScreen(case.width, case.height) && it.size.height >= TOUCH } == true,
            barcode = probe.node(texts.receipt.barcode)?.wholeOnScreen(case.width, case.height) == true,
            name = name,
            reason = reason?.wholeOnScreen(case.width, case.height) == true
        )
    }

    @Test
    fun `главная кнопка, причина и штрихкод видны, наименование не сжато`() {
        val failures = eachWindow { probe, case ->
            val full = measure(probe, case, KassaExtremes.basket(FIFTY), KassaExtremes.fivePayments(), "fifty")
            val empty = measure(probe, case, Basket(), SaleForm(), "empty")
            println(
                "продажа ${case.tag}: кнопка ${full.issue}/${empty.issue}, штрихкод ${full.barcode}, " +
                    "причина ${empty.reason}, наименование ${full.name}"
            )
            listOfNotNull(
                "${case.tag}: «Пробить чек» не видна или ниже цели".takeIf { !full.issue || !empty.issue },
                "${case.tag}: поле штрихкода не видно целиком".takeIf { !full.barcode },
                "${case.tag}: причина под кнопкой не видна".takeIf { !empty.reason },
                "${case.tag}: наименованию ${full.name}".takeIf { full.name < TableColumns.name.value }
            )
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /**
     * Чек на тысячу строк в тёмном оформлении.
     *
     * Лист чека ленивый, и тысяча строк не должна ни задержать экран,
     * ни сдвинуть кассу: кнопка и штрихкод на месте, а тёмная схема
     * не теряет ни одной роли цвета.
     */
    @Test
    fun `тысяча строк в тёмном оформлении не сдвигает кассу`() {
        val cases = listOf(
            Case(WIDE, TALL, TextScale.Normal, Language.Ru),
            Case(LOW, SHORT, TextScale.Larger, Language.Kk)
        )
        val failures = eachWindow(cases, Appearance.Dark) { probe, case ->
            val desk = desk()
            val texts = textsOf(case.language).common
            val started = System.nanoTime()
            probe.show(case.look, case.language) {
                KassaWindow(desk, Section.Sale) {
                    SaleContent(sale(KassaExtremes.basket(THOUSAND), KassaExtremes.fivePayments()))
                }
            }
            val millis = (System.nanoTime() - started) / MILLION
            probe.save("sale-thousand-dark-${case.tag}")
            val issue = probe.node(texts.receipt.issueSale)?.wholeOnScreen(case.width, case.height) == true
            val barcode = probe.node(texts.receipt.barcode)?.wholeOnScreen(case.width, case.height) == true
            println("тысяча строк ${case.tag}: $millis мс, кнопка $issue, штрихкод $barcode")
            listOfNotNull("${case.tag}: касса сдвинута".takeIf { !issue || !barcode })
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /**
     * Суммы оплаты от миллиарда видны целиком, остаток — минусом и разрядами.
     *
     * Поле суммы постоянной ширины резало миллиарды даже в самом широком
     * окне, а отрицательный остаток был единственной суммой с дефисом
     * и без разрядов. Меряется ширина набранного против ширины поля.
     */
    @Test
    fun `суммы оплаты целиком, отрицательный остаток минусом`() {
        val cases = KassaExtremes.CASES.filter { it.language == Language.Ru }
        val failures = eachWindow(cases) { probe, case ->
            val desk = desk()
            val cheap = Basket().add(KassaExtremes.basket(2).positions[1])
            probe.show(case.look, case.language) {
                KassaWindow(desk, Section.Sale) { SaleContent(sale(cheap, KassaExtremes.fivePayments())) }
            }
            // Кадр — с оплатой на виду: касса прокручена от штрихкода вниз.
            probe.node(textsOf(case.language).common.receipt.barcode)?.boundsInRoot?.center
                ?.let { at -> repeat(WHEELS) { probe.wheel(at, SCROLL) } }
            probe.save("sale-payments-${case.tag}")
            val fields = probe.parts().filter { it.config.getOrNull(SemanticsProperties.EditableText) != null }
            val amounts = fields.filter { it.label().any(Char::isDigit) }
            val rest = amounts.firstOrNull { Glyphs.MINUS in it.label() }
            val cut = amounts.filter { (probe.textWidth(it) ?: 0f) > it.size.width - FIELD_INSETS }
            println("оплата ${case.tag}: полей ${amounts.size}, остаток «${rest?.label()}», обрезано ${cut.size}")
            listOfNotNull(
                "${case.tag}: остаток не набран минусом".takeIf { rest == null || Glyphs.NBSP !in rest.label() },
                "${case.tag}: обрезано полей ${cut.size}".takeIf { cut.isNotEmpty() }
            )
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val FIFTY = 50
        const val NAME_PROBE = 20
        const val REASON_PROBE = 6
        const val THOUSAND = 1000
        const val TOUCH = 48
        const val SCROLL = 10f
        const val WHEELS = 8

        /** Поля поля ввода слева и справа от набранного по Material 3. */
        const val FIELD_INSETS = 32
        const val MILLION = 1_000_000
        const val WIDE = 1180
        const val TALL = 820
        const val LOW = 960
        const val SHORT = 640
    }
}
