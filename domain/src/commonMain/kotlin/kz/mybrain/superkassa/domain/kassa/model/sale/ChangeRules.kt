package kz.mybrain.superkassa.domain.kassa.model.sale

/**
 * Чем негодна скидка или наценка на весь чек.
 *
 * Знак, граница процента и размер скидки проверяются вместе: набрано одно
 * число, и кассиру называется одна причина, а не та, что ближе к началу
 * списка. Порядок внутри — от смысла к величине: минус меняет скидку
 * на наценку, и говорить про «больше суммы позиций» о нём бессмысленно.
 *
 * Скидка со знаком минус прибавляла к итогу, наценка со знаком минус
 * вычитала: «Итого» расходилось с набранным, и ни одна строка экрана
 * этого не объясняла.
 */
fun changeBlockOf(state: SaleState): SaleBlock? {
    val discount = state.discount.sumOf(state.itemsSum) ?: 0L
    return when {
        // Набранное не числом не превращается в ноль молча: «10,005» и «abc»
        // прежде исчезали, поле не краснело, и чек уходил по полной цене.
        malformed(state.discount) || malformed(state.markup) -> SaleBlock.ChangeNotANumber
        state.hasItemDiscount && discount > 0L -> SaleBlock.DiscountScopes
        negative(state.discount) || negative(state.markup) -> SaleBlock.DiscountNegative
        overHundred(state.discount) || overHundred(state.markup) -> SaleBlock.PercentOverHundred
        // Сравнение только при набранной скидке: у чека из одних сторно
        // сумма позиций уходит ниже нуля, и ненабранная скидка оказывалась
        // «больше» её — кассир читал «уменьшите скидку» над пустыми полями.
        discount > 0L && discount > state.itemsSum -> SaleBlock.DiscountOverItems
        else -> null
    }
}

/** Набрано ли что-то, что суммой или долей не читается. */
private fun malformed(change: Adjustment): Boolean = change.text.isNotBlank() && change.entered == null

/** Набрано ли число меньше нуля: пустое и недобранное — не минус. */
private fun negative(change: Adjustment): Boolean = (change.entered?.signum ?: 0) < 0

/** Набран ли процент больше ста: у суммы такой границы нет, у доли есть. */
private fun overHundred(change: Adjustment): Boolean {
    val entered = change.entered ?: return false
    return change.unit == AdjustmentUnit.Percent && entered > HUNDRED_PERCENT
}
