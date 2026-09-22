package kz.mybrain.superkassa.desktop.ui.sale

import java.math.BigDecimal

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

    /**
     * Скидка на позицию: сумма в тенге или доля от стоимости строки.
     *
     * Тем же типом, что и скидка на чек: понятие одно, и второй способ
     * его набрать кассиру пришлось бы читать заново.
     */
    val discount: Adjustment = Adjustment(),
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
        DraftField.Discount -> discount.text
    }

    /** Ошибка этого поля, если она есть. */
    fun problem(field: DraftField): DraftProblem? = problems.firstOrNull { it.field == field }

    /**
     * Начата ли форма: кассир в неё что-то набрал.
     *
     * Правило одно на подсветку полей и на строку под кнопкой. Прежде
     * его знали только поля, и над нетронутой формой — при открытии
     * смены, до первого товара — стояло красное «Введите наименование
     * товара» при неподсвеченных полях: кассир читал упрёк за работу,
     * которую ещё не начинал.
     */
    val started: Boolean
        get() = name.isNotBlank() ||
            price.isNotBlank() ||
            discount.text.isNotBlank() ||
            quantity != DEFAULT_QUANTITY

    /** Чего форме не хватает — или `null`, пока кассир ничего не набрал. */
    val hint: DraftProblem?
        get() = problems.firstOrNull()?.takeIf { started }

    /** Готовая позиция или `null`, если введённое ещё не образует позицию. */
    val position: Position?
        get() {
            if (problems.isNotEmpty()) return null
            return Position(
                name = name.trim(),
                price = amount(price).value ?: BigDecimal.ZERO,
                quantity = amount(quantity, QUANTITY_SCALE).value ?: BigDecimal.ONE,
                vatGroup = vatGroup,
                discount = discountSum ?: BigDecimal.ZERO,
                storno = storno,
                measureUnitCode = measureUnitCode
            )
        }

    /**
     * Стоимость строки до скидки: цена на количество.
     *
     * От неё берётся доля скидки и ею же скидка ограничена. `null`, пока
     * цена или количество не набраны числом: доли от неизвестного нет.
     */
    val lineCost: BigDecimal?
        get() {
            val priced = amount(price).value ?: return null
            val counted = amount(quantity, QUANTITY_SCALE).value ?: return null
            return priced.multiply(counted)
        }

    /**
     * Скидка на позицию в тенге — ровно это число уходит в узел.
     *
     * Доля остаётся способом ввода и считается тем же правилом, каким
     * считается скидка на чек: узел принимает и процент, но округлил бы
     * его своим порядком, и строка чека разошлась бы с экраном на тиын.
     */
    val discountSum: BigDecimal?
        get() = discount.sumOf(lineCost ?: return null)

    /** Форма после добавления: наименование и цена очищаются, ставка остаётся. */
    fun cleared(): PositionDraft = PositionDraft(
        vatGroup = vatGroup,
        // Способ ввода скидки остаётся выбранным: следующую скидку
        // кассир обычно даёт тем же — так же, как у скидки на чек.
        discount = Adjustment(unit = discount.unit),
        measureUnitCode = measureUnitCode
    )

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
     * БФД чек ни с отрицательной, ни с нулевой строкой не примет.
     *
     * Отрицательная скидка названа своей причиной. Прежде она считалась
     * той же бедой, что и слишком большая, и кассир, набравший «-100»,
     * читал «скидка не может быть больше стоимости позиции» — при скидке
     * заведомо меньше стоимости.
     */
    private fun discountProblems(): List<DraftProblem> = when (val parsed = amount(discount.text)) {
        is Amount.NotANumber -> listOf(DraftProblem.DiscountNotANumber)
        is Amount.TooPrecise -> listOf(DraftProblem.DiscountTooPrecise)
        is Amount.Empty -> emptyList()
        is Amount.Value -> listOfNotNull(
            DraftProblem.DiscountNegative.takeIf { parsed.amount < BigDecimal.ZERO },
            DraftProblem.DiscountOverPercent.takeIf { overHundred(parsed.amount) },
            DraftProblem.DiscountTooBig.takeIf { tooBig() }
        )
    }

    /** Доля сверх ста процентов отдала бы строку даром и ещё сверху. */
    private fun overHundred(entered: BigDecimal): Boolean =
        discount.unit == AdjustmentUnit.Percent && entered > HUNDRED_PERCENT

    /**
     * Скидка, равная стоимости позиции, — та же беда, что и большая:
     * строка выходит нулевой, а нулевая строка — не продажа. Прежде
     * равенство проходило, и товар за ноль вставал в фискальный чек.
     *
     * Сравнивается посчитанная сумма, а не набранное: сто процентов
     * съедают строку так же начисто, как её полная стоимость в тенге.
     */
    private fun tooBig(): Boolean {
        val cost = lineCost ?: return false
        val given = discount.sumOf(cost) ?: return false
        return given >= cost
    }
}

/** Штучный товар — обычный случай, и количество для него подставлено сразу. */
const val DEFAULT_QUANTITY: String = "1"
