package kz.mybrain.superkassa.presentation.sale

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.strings.SaleTexts
import kz.mybrain.superkassa.presentation.theme.Glyphs
import java.math.BigDecimal

/** Из чего сложилась строка: количество, цена за единицу, ставка и скидка. */
@Composable
internal fun positionDetail(position: Position): String {
    // Единица стоит при количестве: «1,5 × 2 500» и «1,5 кг × 2 500» —
    // разные строки чека, и кассир обязан видеть которая перед ним.
    val unit = unitTitle(LocalUnits.current, position.measureUnitCode)
    // Ставка в строке стоит, только когда касса её выделяет: у
    // неплательщика НДС «Без НДС» в каждой строке чека — шум, а не
    // сведения, и раньше на этом месте стояла ставка, до БФД не дошедшая.
    val rates = LocalVatRates.current
    val vat = if (rates.size < 2) null else vatTitle(rates, position.vatGroup)
    return positionLine(position, unit, vat, LocalSaleTexts.current)
}

/**
 * Состав строки словами — без Compose: разбирается проверкой, а не глазом.
 *
 * Сведения разделяет общий знак набора, а не набранная здесь точка:
 * своя копия давала в листе чека другой зазор, чем в соседних списках
 * приложения, и первая же правка общего знака обошла бы эту строку.
 */
internal fun positionLine(
    position: Position,
    unit: String,
    vat: String?,
    texts: SaleTexts
): String {
    // Количество отделяет дробь тем же знаком, что и деньги: строка
    // «1.450 кг × 3 450,00 ₸» разводила в одном месте две записи числа.
    val counted = listOf(quantityText(position.quantity), unit).filter { it.isNotBlank() }
    val parts = mutableListOf(
        counted.joinToString(" ") + Glyphs.TIMES + Money.format(position.price)
    )
    vat?.let { parts += it }
    if (position.discount > BigDecimal.ZERO) {
        parts += "${texts.lineDiscount} ${Money.format(position.discount)}"
    }
    // Число после слова, а не перед ним: «1 марок» — согласование, которое
    // по-русски верно только при пяти и больше, а марка бывает и одна.
    if (position.exciseStamps.isNotEmpty()) {
        parts += "${texts.exciseCount}: ${position.exciseStamps.size}"
    }
    return parts.joinToString(Glyphs.SEPARATOR)
}

/**
 * Количество товара словами кассира: дробь через запятую и разряды
 * тем же неразрывным пробелом, что и в цене рядом, — «1234,5» при цене
 * «1 234,50» читалось как число другого порядка.
 */
internal fun quantityText(quantity: BigDecimal): String =
    Money.grouped(quantity.toPlainString().replace('.', Glyphs.DECIMAL))
