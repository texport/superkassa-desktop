package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.kassa.model.Percent
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.entry.amount

/**
 * Чем набрана скидка или наценка на чек.
 *
 * Тенге и процент — два способа сказать об одном: покупателю обещают
 * то «пятьсот тенге», то «десять процентов», и переводить второе в первое
 * в уме кассир не должен. Способ ввода живёт при самом числе, а не
 * отдельным переключателем сбоку: на экране их два, и общий переключатель
 * молча менял бы смысл соседнего поля.
 */
enum class AdjustmentUnit { Tenge, Percent }

/**
 * Скидка или наценка на чек: что набрано и в чём.
 *
 * Держится строкой, а не числом: кассир набирает её по знаку, и «10,»
 * в середине набора — ещё не число, но уже не пустота.
 */
data class Adjustment(val text: String = "", val unit: AdjustmentUnit = AdjustmentUnit.Tenge) {

    /** Набранное число или `null`, если набрано пусто либо не число. */
    val entered: Decimal? get() = amount(text).value

    /**
     * Сколько это в тиынах от суммы позиций.
     *
     * Ровно это число уходит в кассу: процент остаётся способом ввода,
     * а чек считается от суммы.
     */
    fun sumOf(itemsSum: Long): Long? {
        val number = entered ?: return null
        return when (unit) {
            AdjustmentUnit.Tenge -> Tenge.of(number)
            AdjustmentUnit.Percent -> Percent.of(itemsSum, number)
        }
    }

    /** Поле очищено, а выбранный способ ввода остался: следующая скидка обычно в том же. */
    fun cleared(): Adjustment = copy(text = "")
}

/**
 * Итог чека со скидкой и наценкой на него, в тиынах.
 *
 * Считается в одном месте: экран, правила и запрос к кассе обязаны получить
 * одно и то же число, а посчитанный трижды заново итог расходился бы с тем,
 * что кассир прочитал на экране.
 */
fun totalOf(basket: Basket, form: SaleForm): Long =
    basket.totalWith(form.discount.sumOf(basket.total), form.markup.sumOf(basket.total))

/**
 * Снимок скидок и наценок чека для блока и его правил.
 *
 * Собран из корзины и набранного: правила о скидках проверяются и на
 * экране, и в проверках одним и тем же кодом.
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
 * Больше ста процентов касса не принимает ни у скидки, ни у наценки.
 *
 * Скидка в сто процентов отдаёт чек даром, а наценка сверх ста — это
 * уже другая цена, и её место в цене позиции.
 */
val HUNDRED_PERCENT: Decimal = Decimal.ofScaled(HUNDRED, 0)

private const val HUNDRED = 100L
