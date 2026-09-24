package kz.mybrain.superkassa.domain.kassa.model.entry

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.sale.Adjustment
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.HUNDRED_PERCENT
import kz.mybrain.superkassa.domain.kassa.model.sale.NO_VAT
import kz.mybrain.superkassa.domain.kassa.model.sale.Position

/**
 * Введённое кассиром до того, как оно стало позицией чека.
 *
 * Проверки живут здесь, а не в форме: их можно прогнать тестом, не поднимая
 * экран, и они одни и те же для ввода с клавиатуры и по Enter. Деньги
 * готовой позиции — в тиынах.
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
     * Без неё касса ставит штуку, и полтора килограмма баранины уходили
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
                price = amount(price).tiyn ?: 0L,
                quantity = amount(quantity, QUANTITY_SCALE).value ?: ONE,
                vatGroup = vatGroup,
                discount = discountSum ?: 0L,
                storno = storno,
                measureUnitCode = measureUnitCode
            )
        }

    /**
     * Стоимость строки до скидки: цена на количество, к ближайшему тиыну.
     *
     * От неё берётся доля скидки и ею же скидка ограничена — той же
     * стоимостью, от которой долю берёт касса. `null`, пока цена или
     * количество не набраны числом: доли от неизвестного нет.
     */
    val lineCost: Long?
        get() {
            val priced = amount(price).tiyn ?: return null
            val counted = amount(quantity, QUANTITY_SCALE).value ?: return null
            return Tenge.lineSum(priced, counted)
        }

    /**
     * Скидка на позицию в тиынах — ровно это число уходит в кассу.
     *
     * Доля остаётся способом ввода и считается тем же правилом, каким
     * считается скидка на чек: от стоимости строки к ближайшему тиыну.
     */
    val discountSum: Long?
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
            DraftProblem.PriceNotPositive.takeIf { parsed.amount.signum <= 0 }
        )
    }

    private fun quantityProblems(): List<DraftProblem> =
        when (val parsed = amount(quantity, QUANTITY_SCALE)) {
            is Amount.NotANumber -> listOf(DraftProblem.QuantityNotANumber)
            is Amount.TooPrecise -> listOf(DraftProblem.QuantityTooPrecise)
            is Amount.Empty -> listOf(DraftProblem.QuantityNotPositive)
            is Amount.Value -> listOfNotNull(
                DraftProblem.QuantityNotPositive.takeIf { parsed.amount.signum <= 0 },
                DraftProblem.QuantityNotWhole.takeIf { fractional(parsed.amount) }
            )
        }

    /** Дробное количество у штучного товара: полторы штуки не продают. */
    private fun fractional(counted: Decimal): Boolean =
        wholeOnly(measureUnitCode) && counted.scaled(QUANTITY_SCALE) % WHOLE != 0L

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
            DraftProblem.DiscountNegative.takeIf { parsed.amount.signum < 0 },
            DraftProblem.DiscountOverPercent.takeIf { overHundred(parsed.amount) },
            DraftProblem.DiscountTooBig.takeIf { tooBig() }
        )
    }

    /** Доля сверх ста процентов отдала бы строку даром и ещё сверху. */
    private fun overHundred(entered: Decimal): Boolean =
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

/** Одна единица товара. */
private val ONE: Decimal = Decimal.ofScaled(1L, 0)

/** Тысячных в единице количества. */
private const val WHOLE: Long = 1_000L
