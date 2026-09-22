package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleSection
import kz.mybrain.superkassa.desktop.ui.components.MinorSumLine
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.MoneyField
import kz.mybrain.superkassa.desktop.ui.components.NamedSumRow
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Скидки и наценки чека — одним блоком.
 *
 * Прежде поля стояли среди реквизитов чека, а их след — среди итогов:
 * кассир, давший скидку, искал её сумму в другом углу экрана, чем поле,
 * в которое её набрал. Здесь набранное и его последствие стоят рядом,
 * и «было — стало» читается сверху вниз.
 *
 * Скидка на позицию остаётся у позиции, но названа здесь строкой: она
 * и скидка на чек вместе запрещены, и сколько уже дано по строкам,
 * кассир обязан видеть до того, как наберёт скидку на весь чек.
 */
@Composable
fun ReceiptChangesCard(
    session: Session,
    form: SaleForm,
    basket: Basket,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val state = changesOf(basket, form)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CollapsibleSection(title = extra.receiptChanges, expanded = expanded, onToggle = onToggle) {
                // Краснеет то поле, в котором набрана помеха, а не оба:
                // причина под ними одна, и найти по ней своё поле кассир
                // должен взглядом.
                ChangeField(
                    label = texts.sale.receiptDiscount,
                    change = form.discount,
                    itemsSum = basket.total,
                    wrong = state.discountWrong(),
                    onEnter = form::enterDiscount,
                    onSwitch = form::switchDiscount
                )
                ChangeField(
                    label = texts.sale.receiptMarkup,
                    change = form.markup,
                    itemsSum = basket.total,
                    wrong = state.markupWrong(),
                    onEnter = form::enterMarkup,
                    onSwitch = form::switchMarkup
                )
                Hint(
                    problem = changeBlockOf(state)
                        ?.reason(extra, paymentTexts(session.language)),
                    hint = texts.sale.discountOrMarkup
                )
                HorizontalDivider()
                ChangeSummary(form, basket)
            }
        }
    }
}

/**
 * Поле скидки или наценки со способом ввода внутри него.
 *
 * Знак стоит в самом поле, а не переключателем сбоку: полей два, и общий
 * переключатель менял бы смысл соседнего молча. Под полем написано то же
 * число другим способом — набравший процент видит тенге, набравший
 * тенге видит долю.
 */
@Composable
private fun ChangeField(
    label: String,
    change: Adjustment,
    itemsSum: BigDecimal,
    wrong: Boolean,
    onEnter: (String) -> Unit,
    onSwitch: (AdjustmentUnit) -> Unit
) {
    MoneyField(
        value = change.text,
        label = label,
        modifier = Modifier.fillMaxWidth(),
        isError = wrong,
        supportingText = sameOtherwise(change, itemsSum),
        trailing = { UnitChoice(change.unit, onSwitch) },
        onValueChange = onEnter
    )
}

/** Тенге или процент: оба знака видны сразу, и выбранный читается без списка. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitChoice(selected: AdjustmentUnit, onSwitch: (AdjustmentUnit) -> Unit) {
    ChoiceSegments(
        options = AdjustmentUnit.entries,
        selected = selected,
        label = { it.sign },
        onSelect = onSwitch
    )
}

/**
 * То же число другим способом — или `null`, пока набирать нечего.
 *
 * Именно `null`, а не пустая строка: строка занимает место под полем
 * всегда, и ненабранная скидка отодвигала наценку на полтора шага
 * дальше, чем отстоят друг от друга поля соседних карточек.
 */
@Composable
private fun sameOtherwise(change: Adjustment, itemsSum: BigDecimal): String? {
    val extra = LocalSaleTexts.current
    val entered = change.entered ?: return null
    return when (change.unit) {
        AdjustmentUnit.Percent -> extra.changeAsSum.format(Money.format(tengeOfPercent(itemsSum, entered)))
        AdjustmentUnit.Tenge ->
            percentOfTenge(itemsSum, entered)?.let { extra.changeAsPercent.format(formatPercent(it)) }
    }
}

/**
 * «Было — стало» чека.
 *
 * «Было» — стоимость набранного до всяких скидок, «стало» — то, что
 * кассир назовёт покупателю. Между ними строками стоит всё, что развело
 * эти два числа, включая скидку по позициям: без неё «было» и «стало»
 * расходились бы на сумму, которой на экране нет.
 */
@Composable
private fun ChangeSummary(form: SaleForm, basket: Basket) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val discount = form.discount.sumOf(basket.total)
    val markup = form.markup.sumOf(basket.total)
    val given = basket.itemDiscounts
    MinorSumLine(extra.changesBefore, Money.format(basket.total + given))
    if (given.signum() != 0) MinorSumLine(extra.itemDiscountsGiven, Money.format(given.negate()))
    if (discount != null && discount.signum() > 0) {
        MinorSumLine(changeTitle(texts.sale.receiptDiscount, form.discount), Money.format(discount.negate()))
    }
    if (markup != null && markup.signum() > 0) {
        MinorSumLine(changeTitle(texts.sale.receiptMarkup, form.markup), Money.format(markup))
    }
    NamedSumRow(name = extra.changesAfter, amount = Money.format(basket.totalWith(discount, markup)))
}

/**
 * Подпись строки скидки или наценки.
 *
 * Набранный процент назван в ней же: иначе строка «Скидка на чек —
 * 1 137,25 ₸» не объясняла бы, откуда взялось это число.
 */
private fun changeTitle(label: String, change: Adjustment): String {
    val percent = change.entered?.takeIf { change.unit == AdjustmentUnit.Percent } ?: return label
    return "$label${Glyphs.SEPARATOR}${formatPercent(percent)}"
}
