package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.math.BigDecimal

/** Разобранное число: пусто, не число, слишком мелко — или значение. */
sealed interface Amount {
    data object Empty : Amount
    data object NotANumber : Amount
    data object TooPrecise : Amount
    data class Value(val amount: BigDecimal) : Amount

    val value: BigDecimal? get() = (this as? Value)?.amount
}

/**
 * Разбор введённого числа.
 *
 * Принимаются и запятая, и точка, и оба вида пробела: кассир вставляет
 * сумму из отчёта, где разряды разделены неразрывным пробелом. Отдельно
 * от «не число» различается «мельче допустимого» — это разные ошибки
 * и разные подсказки.
 */
fun amount(text: String, maxScale: Int = Money.TIYN_SCALE): Amount {
    val normalized = text.replace(',', '.').filterNot { it == ' ' || it == Glyphs.NBSP }.trim()
    if (normalized.isEmpty()) return Amount.Empty
    val parsed = normalized.toBigDecimalOrNull() ?: return Amount.NotANumber
    return if (parsed.scale() > maxScale) Amount.TooPrecise else Amount.Value(parsed)
}

/** Весовой товар взвешивается до грамма: тысячная доля килограмма. */
const val QUANTITY_SCALE: Int = 3
