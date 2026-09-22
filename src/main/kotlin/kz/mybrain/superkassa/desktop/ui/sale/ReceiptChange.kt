package kz.mybrain.superkassa.desktop.ui.sale

import java.math.BigDecimal

/**
 * Итог чека со скидкой и наценкой на него.
 *
 * Считается в одном месте: экран, правила и запрос к узлу обязаны
 * получить одно и то же число, а посчитанный трижды заново итог
 * расходился бы с тем, что кассир прочитал на экране.
 */
fun totalOf(basket: Basket, form: SaleForm): BigDecimal =
    basket.totalWith(amount(form.discount).value, amount(form.markup).value)
