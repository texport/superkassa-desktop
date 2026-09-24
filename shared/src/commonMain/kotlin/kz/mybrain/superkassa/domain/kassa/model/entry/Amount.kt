package kz.mybrain.superkassa.domain.kassa.model.entry

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.kassa.model.Tenge

/** Разобранное число: пусто, не число, слишком мелко — или значение. */
sealed interface Amount {
    data object Empty : Amount
    data object NotANumber : Amount
    data object TooPrecise : Amount
    data class Value(val amount: Decimal) : Amount

    val value: Decimal? get() = (this as? Value)?.amount

    /** Значение в тиынах; у числа мельче тиына доля округляется, как у ядра. */
    val tiyn: Long? get() = value?.let(Tenge::of)
}

/**
 * Разбор введённого числа.
 *
 * Принимаются и запятая, и точка, и оба вида пробела: кассир вставляет
 * сумму из отчёта, где разряды разделены неразрывным пробелом. Отдельно
 * от «не число» различается «мельче допустимого» — это разные ошибки
 * и разные подсказки. Запись с порядком («1E3») — не число: кассир её
 * не набирает, а слипшийся ввод сканера так читался бы тысячей.
 */
fun amount(text: String, maxScale: Int = Tenge.TIYN_SCALE): Amount {
    val normalized = text.replace(',', '.').filterNot { it == ' ' || it == NBSP }.trim()
    return when {
        normalized.isEmpty() -> Amount.Empty
        !NUMBER.matches(normalized) -> Amount.NotANumber
        normalized.substringAfter('.', "").length > maxScale -> Amount.TooPrecise
        else -> valueOf(normalized) ?: Amount.NotANumber
    }
}

/** Число из записи, уже признанной числом; `null` — не помещается в целое. */
private fun valueOf(normalized: String): Amount.Value? {
    val body = normalized.trimStart('+', '-')
    val fraction = body.substringAfter('.', "")
    val digits = (body.substringBefore('.') + fraction).trimStart('0').ifEmpty { "0" }
    if (digits.length > MAX_DIGITS) return null
    val unscaled = digits.toLong().let { if (normalized.startsWith('-')) -it else it }
    return Amount.Value(Decimal.ofScaled(unscaled, fraction.length))
}

/** Весовой товар взвешивается до грамма: тысячная доля килограмма. */
const val QUANTITY_SCALE: Int = 3

/** Число со знаком: цифры, точка и цифры; хотя бы одна цифра. */
private val NUMBER = Regex("""[+-]?(\d+\.?\d*|\.\d+)""")

/** Столько цифр помещается в целое число без переполнения. */
private const val MAX_DIGITS = 18

private const val NBSP = '\u00A0'
