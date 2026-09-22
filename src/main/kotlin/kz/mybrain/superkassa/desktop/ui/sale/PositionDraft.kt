package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.math.BigDecimal

/** Поле формы ввода позиции: по нему подсвечивается ошибка. */
enum class DraftField { Name, Price, Quantity, Discount }

/**
 * Что не так с введённой позицией.
 *
 * «Не число» и «слишком мелко» разведены намеренно: кассир, набравший
 * количество 1,234 килограмма, должен узнать про предел точности, а не
 * гадать, чем ему не угодила цифра.
 */
enum class DraftProblem(val field: DraftField, val text: (SaleTexts) -> String) {
    NameMissing(DraftField.Name, { it.needName }),
    PriceNotANumber(DraftField.Price, { it.notANumber }),
    PriceTooPrecise(DraftField.Price, { it.pricePrecision }),
    PriceNotPositive(DraftField.Price, { it.needPrice }),
    QuantityNotANumber(DraftField.Quantity, { it.notANumber }),
    QuantityTooPrecise(DraftField.Quantity, { it.quantityPrecision }),
    QuantityNotPositive(DraftField.Quantity, { it.needQuantity }),
    QuantityNotWhole(DraftField.Quantity, { it.quantityWhole }),
    DiscountNotANumber(DraftField.Discount, { it.notANumber }),
    DiscountTooPrecise(DraftField.Discount, { it.pricePrecision }),
    DiscountTooBig(DraftField.Discount, { it.discountTooBig }),
    DiscountNegative(DraftField.Discount, { it.discountNegative })
}

/**
 * Введённое кассиром до того, как оно стало позицией чека.
 *
 * Проверки живут здесь, а не в форме: их можно прогнать тестом, не поднимая
 * экран, и они одни и те же для ввода с клавиатуры и по Enter.
 */
data class PositionDraft(
    val name: String = "",
    val price: String = "",
    val quantity: String = DEFAULT_QUANTITY,
    val vatGroup: String = NO_VAT,
    val discount: String = "",
    val storno: Boolean = false,
    /**
     * Единица измерения по ОКЕИ.
     *
     * Без неё узел ставит штуку, и полтора килограмма баранины уходили
     * в ОФД как «1,5 шт». Количество протокол везёт в тысячных долях,
     * то есть весовой товар в чеке допустим — недоставало только единицы.
     */
    val measureUnitCode: String? = null
) {
    val problems: List<DraftProblem>
        get() = buildList {
            if (name.isBlank()) add(DraftProblem.NameMissing)
            addAll(priceProblems())
            addAll(quantityProblems())
            addAll(discountProblems())
        }

    /** Введённое в это поле. */
    fun valueOf(field: DraftField): String = when (field) {
        DraftField.Name -> name
        DraftField.Price -> price
        DraftField.Quantity -> quantity
        DraftField.Discount -> discount
    }

    /** Ошибка этого поля, если она есть. */
    fun problem(field: DraftField): DraftProblem? = problems.firstOrNull { it.field == field }

    /** Готовая позиция или `null`, если введённое ещё не образует позицию. */
    val position: Position?
        get() {
            if (problems.isNotEmpty()) return null
            return Position(
                name = name.trim(),
                price = amount(price).value ?: BigDecimal.ZERO,
                quantity = amount(quantity, QUANTITY_SCALE).value ?: BigDecimal.ONE,
                vatGroup = vatGroup,
                discount = amount(discount).value ?: BigDecimal.ZERO,
                storno = storno,
                measureUnitCode = measureUnitCode
            )
        }

    /** Форма после добавления: наименование и цена очищаются, ставка остаётся. */
    fun cleared(): PositionDraft = PositionDraft(vatGroup = vatGroup, measureUnitCode = measureUnitCode)

    private fun priceProblems(): List<DraftProblem> = when (val parsed = amount(price)) {
        is Amount.NotANumber -> listOf(DraftProblem.PriceNotANumber)
        is Amount.TooPrecise -> listOf(DraftProblem.PriceTooPrecise)
        is Amount.Empty -> listOf(DraftProblem.PriceNotPositive)
        is Amount.Value -> listOfNotNull(
            DraftProblem.PriceNotPositive.takeIf { parsed.amount <= BigDecimal.ZERO }
        )
    }

    private fun quantityProblems(): List<DraftProblem> =
        when (val parsed = amount(quantity, QUANTITY_SCALE)) {
            is Amount.NotANumber -> listOf(DraftProblem.QuantityNotANumber)
            is Amount.TooPrecise -> listOf(DraftProblem.QuantityTooPrecise)
            is Amount.Empty -> listOf(DraftProblem.QuantityNotPositive)
            is Amount.Value -> listOfNotNull(
                DraftProblem.QuantityNotPositive.takeIf { parsed.amount <= BigDecimal.ZERO },
                DraftProblem.QuantityNotWhole.takeIf { fractional(parsed.amount) }
            )
        }

    /** Дробное количество у штучного товара: полторы штуки не продают. */
    private fun fractional(counted: BigDecimal): Boolean =
        wholeOnly(measureUnitCode) && counted.stripTrailingZeros().scale() > 0

    /**
     * Скидка на позицию необязательна, но не может съесть позицию целиком:
     * ОФД чек с отрицательной строкой не примет.
     *
     * Отрицательная скидка названа своей причиной. Прежде она считалась
     * той же бедой, что и слишком большая, и кассир, набравший «-100»,
     * читал «скидка не может быть больше стоимости позиции» — при скидке
     * заведомо меньше стоимости.
     */
    private fun discountProblems(): List<DraftProblem> = when (val parsed = amount(discount)) {
        is Amount.NotANumber -> listOf(DraftProblem.DiscountNotANumber)
        is Amount.TooPrecise -> listOf(DraftProblem.DiscountTooPrecise)
        is Amount.Empty -> emptyList()
        is Amount.Value -> listOfNotNull(
            DraftProblem.DiscountNegative.takeIf { parsed.amount < BigDecimal.ZERO },
            DraftProblem.DiscountTooBig.takeIf { tooBig(parsed.amount) }
        )
    }

    private fun tooBig(value: BigDecimal): Boolean {
        val priced = amount(price).value ?: return false
        val counted = amount(quantity, QUANTITY_SCALE).value ?: return false
        return value > priced.multiply(counted)
    }
}

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

/** Штучный товар — обычный случай, и количество для него подставлено сразу. */
const val DEFAULT_QUANTITY: String = "1"
