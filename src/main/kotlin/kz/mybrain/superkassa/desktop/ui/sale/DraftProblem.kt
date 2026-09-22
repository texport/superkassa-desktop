package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts

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
    DiscountNegative(DraftField.Discount, { it.discountNegative }),

    /** Доля сверх ста процентов: названа теми же словами, что и у чека. */
    DiscountOverPercent(DraftField.Discount, { it.blockPercentRange })
}
