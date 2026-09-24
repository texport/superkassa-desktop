package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.model.sale.Adjustment
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleState
import kz.mybrain.superkassa.domain.kassa.model.sale.changeBlockOf
import kz.mybrain.superkassa.domain.kassa.model.sale.changesOf
import kz.mybrain.superkassa.domain.kassa.model.sale.discountWrong
import kz.mybrain.superkassa.domain.kassa.model.sale.markupWrong
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.section.CollapsibleSection
import kz.mybrain.superkassa.presentation.common.section.MinorSumLine
import kz.mybrain.superkassa.presentation.common.section.NamedSumRow
import kz.mybrain.superkassa.presentation.kassa.sale.FormActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.paymentTexts
import kz.mybrain.superkassa.presentation.strings.kassa.reason
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
 *
 * У плательщика НДС здесь же выбирают, как задан НДС чека: это тот же
 * выбор «на весь чек или по позициям», что у скидки.
 */
@Composable
fun ReceiptChangesCard(sale: SaleUiState, actions: FormActions, expanded: Boolean, onToggle: () -> Unit) {
    val extra = LocalSaleTexts.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
        ) {
            val title = if (sale.vatPayer) extra.receiptChangesVat else extra.receiptChanges
            CollapsibleSection(title = title, expanded = expanded, onToggle = onToggle) {
                ChangeFields(sale, actions)
                if (sale.vatPayer) ReceiptVatFields(sale, actions.vat)
                HorizontalDivider()
                ChangeSummary(sale.form, sale.basket)
            }
        }
    }
}

/**
 * Поля скидки и наценки и причина под ними.
 *
 * Краснеет то поле, в котором набрана помеха, а не оба: причина под ними
 * одна, и найти по ней своё поле кассир должен взглядом.
 */
@Composable
private fun ChangeFields(sale: SaleUiState, actions: FormActions) {
    val state = changesOf(sale.basket, sale.form)
    DiscountField(sale, state, actions)
    MarkupField(sale, state, actions)
    Hint(
        problem = changeBlockOf(state)?.reason(LocalSaleTexts.current, paymentTexts(LocalLanguage.current)),
        hint = LocalStrings.current.sale.discountOrMarkup
    )
}

@Composable
private fun DiscountField(sale: SaleUiState, state: SaleState, actions: FormActions) {
    AdjustmentField(
        label = LocalStrings.current.sale.receiptDiscount,
        change = sale.form.discount,
        modifier = Modifier.fillMaxWidth(),
        isError = state.discountWrong(),
        supportingText = sameOtherwise(sale.form.discount, sale.basket.total, LocalSaleTexts.current.changeAsPercent),
        onEnter = actions::discount,
        onSwitch = actions::discountUnit
    )
}

@Composable
private fun MarkupField(sale: SaleUiState, state: SaleState, actions: FormActions) {
    AdjustmentField(
        label = LocalStrings.current.sale.receiptMarkup,
        change = sale.form.markup,
        modifier = Modifier.fillMaxWidth(),
        isError = state.markupWrong(),
        supportingText = sameOtherwise(sale.form.markup, sale.basket.total, LocalSaleTexts.current.changeAsPercent),
        onEnter = actions::markup,
        onSwitch = actions::markupUnit
    )
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
    MinorSumLine(extra.changesBefore, Money.formatTiyn(basket.total + given))
    if (given != 0L) MinorSumLine(extra.itemDiscountsGiven, Money.formatTiyn(-given))
    if (discount != null && discount > 0L) {
        MinorSumLine(changeTitle(texts.sale.receiptDiscount, form.discount), Money.formatTiyn(-discount))
    }
    if (markup != null && markup > 0L) {
        MinorSumLine(changeTitle(texts.sale.receiptMarkup, form.markup), Money.formatTiyn(markup))
    }
    NamedSumRow(name = extra.changesAfter, amount = Money.formatTiyn(basket.totalWith(discount, markup)))
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
