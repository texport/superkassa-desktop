package kz.mybrain.superkassa.domain.kassa.model.entry

/** Поле формы ввода позиции: по нему подсвечивается ошибка. */
enum class DraftField { Name, Price, Quantity, Discount }

/**
 * Что не так с введённой позицией.
 *
 * «Не число» и «слишком мелко» разведены намеренно: кассир, набравший
 * количество 1,234 килограмма, должен узнать про предел точности, а не
 * гадать, чем ему не угодила цифра. Словами кассира беду называют
 * надписи области.
 */
enum class DraftProblem(val field: DraftField) {
    NameMissing(DraftField.Name),
    PriceNotANumber(DraftField.Price),
    PriceTooPrecise(DraftField.Price),
    PriceNotPositive(DraftField.Price),
    QuantityNotANumber(DraftField.Quantity),
    QuantityTooPrecise(DraftField.Quantity),
    QuantityNotPositive(DraftField.Quantity),
    QuantityNotWhole(DraftField.Quantity),
    DiscountNotANumber(DraftField.Discount),
    DiscountTooPrecise(DraftField.Discount),
    DiscountTooBig(DraftField.Discount),
    DiscountNegative(DraftField.Discount),

    /** Доля сверх ста процентов: названа теми же словами, что и у чека. */
    DiscountOverPercent(DraftField.Discount)
}
