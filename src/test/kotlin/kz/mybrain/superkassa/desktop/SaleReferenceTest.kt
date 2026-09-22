package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.CompositionLocalProvider
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.sale.LocalVatRates
import kz.mybrain.superkassa.desktop.ui.sale.PositionDraft
import kz.mybrain.superkassa.desktop.ui.sale.UnitPicker
import kz.mybrain.superkassa.desktop.ui.sale.VatPicker
import kz.mybrain.superkassa.desktop.ui.sale.VatRate
import kz.mybrain.superkassa.desktop.ui.sale.unitTitle
import kz.mybrain.superkassa.desktop.ui.sale.vatTitle
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Единица измерения и величина ставки в позиции чека.
 *
 * Количество протокол везёт в тысячных долях, поэтому весовой товар в чеке
 * допустим — недоставало только единицы измерения: без неё узел ставит
 * штуку, и полтора килограмма уходили в ОФД как «1,5 шт».
 */
class SaleReferenceTest {

    private val units = listOf(
        UnitOfMeasurement("796", "шт", "Штука"),
        UnitOfMeasurement("116", "кг", "Килограмм")
    )

    @Test
    fun `единица из формы доходит до позиции чека`() {
        val draft = PositionDraft(
            name = "Баранина",
            price = "2500",
            quantity = "1.5",
            measureUnitCode = "116"
        )

        val position = draft.position

        assertEquals("116", position?.measureUnitCode)
        assertEquals(0, BigDecimal("1.5").compareTo(position?.quantity))
    }

    /**
     * Протокол везёт количество в тысячных долях: 1000 == 1,0.
     * Четвёртый знак после запятой в них не укладывается, и касса обязана
     * не пустить его в чек, а не округлить молча.
     */
    @Test
    fun `количество точнее тысячной доли позицией не становится`() {
        val draft = PositionDraft(name = "Баранина", price = "2500", quantity = "1.2345")

        assertNull(draft.position)
    }

    @Test
    fun `очистка формы сохраняет выбранную единицу`() {
        val draft = PositionDraft(name = "Баранина", price = "2500", measureUnitCode = "116")

        assertEquals("116", draft.cleared().measureUnitCode)
    }

    @Test
    fun `единица названа кратко, незнакомый код показан как есть`() {
        assertEquals("кг", unitTitle(units, "116"))
        assertEquals("5114", unitTitle(units, "5114"))
        assertEquals("", unitTitle(units, null))
        assertEquals("", unitTitle(units, " "))
    }

    @Test
    fun `величина ставки дописывается только когда её нет в названии`() {
        val rates = listOf(
            VatRate("VAT_16", "НДС 16%", 16),
            VatRate("VAT_0", "Нулевая ставка", 0),
            VatRate("NO_VAT", "Без НДС")
        )

        assertEquals("НДС 16%", vatTitle(rates, "VAT_16"))
        assertEquals("Нулевая ставка 0%", vatTitle(rates, "VAT_0"))
        assertEquals("Без НДС", vatTitle(rates, "NO_VAT"))
        assertEquals("VAT_12", vatTitle(rates, "VAT_12"))
    }

    /**
     * «Без НДС» — не нулевая ставка.
     *
     * Узел присылает при этой группе ноль процентов, и касса дописывала
     * его к названию: «Без НДС 0%» кассир читает как обложение по нулевой
     * ставке, а это другая строка чека, чем товар вне НДС.
     */
    @Test
    fun `без НДС не выдаётся за нулевую ставку`() {
        val rates = listOf(VatRate("NO_VAT", "Без НДС", 0), VatRate("VAT_0", "Нулевая ставка", 0))

        assertEquals("Без НДС", vatTitle(rates, "NO_VAT"))
        assertEquals("Нулевая ставка 0%", vatTitle(rates, "VAT_0"))
    }

    /**
     * Ставка и единица, которых нет в справочнике узла.
     *
     * Поле показывало на такой код пустоту, и «Без НДС» кассы, названное
     * узлом другим кодом, выглядело незаполненной ставкой — притом что
     * в чек этот код уходил. Кадры сравниваются с полем, где не выбрано
     * ничего: пустое и незнакомое обязаны выглядеть по-разному.
     */
    @Test
    fun `незнакомый код показан в поле, а не стёрт из него`() {
        val known = shot { VatPicker(selected = "VAT_99", onSelect = {}) }
        val nothing = shot { VatPicker(selected = "", onSelect = {}) }
        assertNotEquals(nothing.toList(), known.toList(), "незнакомая ставка неотличима от невыбранной")

        val unit = shot { UnitPicker(selected = "5114", units = units, onSelect = {}) }
        val noUnit = shot { UnitPicker(selected = "", units = units, onSelect = {}) }
        assertNotEquals(noUnit.toList(), unit.toList(), "незнакомая единица неотличима от невыбранной")
    }

    /** Поле выбора в кадре: перечень ставок тот же, что у экрана продажи. */
    private fun shot(content: @androidx.compose.runtime.Composable () -> Unit): ByteArray =
        RenderProbe(PICKER_WIDTH, PICKER_HEIGHT) {
            CompositionLocalProvider(LocalVatRates provides RATES) { content() }
        }.use { probe ->
            repeat(FRAMES) { probe.frame() }
            probe.frame()
        }

    private companion object {
        const val PICKER_WIDTH = 320
        const val PICKER_HEIGHT = 120
        const val FRAMES = 8

        val RATES = listOf(VatRate("NO_VAT", "Без НДС"), VatRate("VAT_16", "НДС 16%", 16))
    }
}
