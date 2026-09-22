package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Чем набрана скидка или наценка на чек.
 *
 * Тенге и процент — два способа сказать об одном: покупателю обещают
 * то «пятьсот тенге», то «десять процентов», и переводить второе в первое
 * в уме кассир не должен. Способ ввода живёт при самом числе, а не
 * отдельным переключателем сбоку: на экране их два, и общий переключатель
 * молча менял бы смысл соседнего поля.
 *
 * Знаки берутся из общего набора: своя копия тенге однажды уже разошлась
 * с той, которой подписаны суммы.
 */
enum class AdjustmentUnit(val sign: String) {
    Tenge(Glyphs.TENGE),
    Percent(Glyphs.PERCENT)
}

/**
 * Скидка или наценка на чек: что набрано и в чём.
 *
 * Держится строкой, а не числом: кассир набирает её по знаку, и «10,»
 * в середине набора — ещё не число, но уже не пустота.
 */
data class Adjustment(val text: String = "", val unit: AdjustmentUnit = AdjustmentUnit.Tenge) {

    /** Набранное число или `null`, если набрано пусто либо не число. */
    val entered: BigDecimal? get() = amount(text).value

    /**
     * Сколько это в тенге от суммы позиций.
     *
     * Ровно это число уходит в узел: процент остаётся способом ввода,
     * а чек считается от суммы.
     */
    fun sumOf(itemsSum: BigDecimal): BigDecimal? = when (unit) {
        AdjustmentUnit.Tenge -> entered
        AdjustmentUnit.Percent -> entered?.let { tengeOfPercent(itemsSum, it) }
    }

    /** Поле очищено, а выбранный способ ввода остался: следующая скидка обычно в том же. */
    fun cleared(): Adjustment = copy(text = "")
}

/**
 * Процент от суммы позиций — в тенге.
 *
 * Округление объявлено здесь и одно на скидку и наценку: до тиына,
 * к ближайшему, половина вверх. Тем же правилом считает узел, и расхождение
 * в один тиын — это расхождение с БФД, а не мелочь на экране.
 */
fun tengeOfPercent(itemsSum: BigDecimal, percent: BigDecimal): BigDecimal =
    itemsSum.multiply(percent).divide(HUNDRED_PERCENT, Money.TIYN_SCALE, RoundingMode.HALF_UP)

/**
 * Какую долю суммы позиций составляет набранная сумма.
 *
 * Нужна не расчёту, а кассиру: набрав скидку тенге, он тут же видит,
 * сколько это процентов, и не пересчитывает их в уме перед покупателем.
 * У пустого чека доли нет — делить не на что.
 */
fun percentOfTenge(itemsSum: BigDecimal, sum: BigDecimal): BigDecimal? =
    if (itemsSum.signum() <= 0) {
        null
    } else {
        sum.multiply(HUNDRED_PERCENT).divide(itemsSum, PERCENT_SCALE, RoundingMode.HALF_UP)
    }

/**
 * Доля в процентах словами экрана.
 *
 * Лишние нули отброшены: «10 %» вместо «10,00 %». Знак держится при числе
 * неразрывным пробелом, как и валюта у суммы.
 */
fun formatPercent(value: BigDecimal): String {
    val plain = value.stripTrailingZeros().toPlainString().replace('.', Glyphs.DECIMAL)
    return "$plain${Glyphs.NBSP}${Glyphs.PERCENT}"
}

/**
 * Итог чека со скидкой и наценкой на него.
 *
 * Считается в одном месте: экран, правила и запрос к узлу обязаны получить
 * одно и то же число, а посчитанный трижды заново итог расходился бы с тем,
 * что кассир прочитал на экране.
 */
fun totalOf(basket: Basket, form: SaleForm): BigDecimal =
    basket.totalWith(form.discount.sumOf(basket.total), form.markup.sumOf(basket.total))

/**
 * Снимок скидок и наценок чека для блока и его правил.
 *
 * Собран из корзины и набранного, без Compose: правила о скидках
 * проверяются и на экране, и в проверках одним и тем же кодом.
 */
fun changesOf(basket: Basket, form: SaleForm): SaleState = SaleState(
    hasItemDiscount = basket.hasItemDiscount,
    discount = form.discount,
    markup = form.markup,
    itemsSum = basket.total
)

/**
 * Мешает ли пробить чек именно скидка — и именно наценка.
 *
 * Причина под полями одна, а красным отмечается то поле, в котором она
 * набрана: два красных поля на одну помеху отправляли кассира искать
 * ошибку в том, где её нет.
 */
fun SaleState.discountWrong(): Boolean = changeBlockOf(copy(markup = Adjustment())) != null

fun SaleState.markupWrong(): Boolean = changeBlockOf(copy(discount = Adjustment())) != null

/**
 * Больше ста процентов узел не принимает ни у скидки, ни у наценки.
 *
 * Скидка в сто процентов отдаёт чек даром, а наценка сверх ста — это
 * уже другая цена, и её место в цене позиции.
 */
val HUNDRED_PERCENT: BigDecimal = BigDecimal(100)

/** Доля показывается до сотой процента: мельче кассир её не набирает. */
private const val PERCENT_SCALE: Int = 2
