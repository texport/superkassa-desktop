package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleSection
import kz.mybrain.superkassa.desktop.ui.components.MinorSumLine
import kz.mybrain.superkassa.desktop.ui.components.NamedSumRow
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
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
fun ReceiptChangesCard(form: SaleForm, basket: Basket, expanded: Boolean, onToggle: () -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val conflict = basket.hasItemDiscount && positive(form.discount)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CollapsibleSection(title = extra.receiptChanges, expanded = expanded, onToggle = onToggle) {
                ChangeField(texts.sale.receiptDiscount, form.discount, conflict || below(form.discount), form::enterDiscount)
                ChangeField(texts.sale.receiptMarkup, form.markup, below(form.markup), form::enterMarkup)
                Hint(
                    problem = when {
                        below(form.discount) || below(form.markup) -> extra.blockDiscountNegative
                        conflict -> extra.blockDiscountScopes
                        else -> null
                    },
                    hint = texts.sale.discountOrMarkup
                )
                HorizontalDivider()
                ChangeSummary(form, basket)
            }
        }
    }
}

/** Поле скидки или наценки на чек: деньги набираются денежным шрифтом. */
@Composable
private fun ChangeField(label: String, value: String, wrong: Boolean, onEnter: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onEnter,
        label = { Text(label) },
        singleLine = true,
        textStyle = MoneyStyle.row,
        isError = wrong,
        modifier = Modifier.fillMaxWidth()
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
    val discount = amount(form.discount).value
    val markup = amount(form.markup).value
    val given = basket.itemDiscounts
    MinorSumLine(extra.changesBefore, formatSigned(basket.total + given))
    if (given.signum() != 0) MinorSumLine(extra.itemDiscountsGiven, formatSigned(given.negate()))
    if (discount != null && discount.signum() > 0) {
        MinorSumLine(texts.sale.receiptDiscount, formatSigned(discount.negate()))
    }
    if (markup != null && markup.signum() > 0) {
        MinorSumLine(texts.sale.receiptMarkup, formatSigned(markup))
    }
    NamedSumRow(name = extra.changesAfter, amount = formatSigned(basket.totalWith(discount, markup)))
}

/** Набрано ли в поле число меньше нуля: пустое и недобранное — не минус. */
private fun below(text: String): Boolean {
    val value = amount(text).value ?: return false
    return value < BigDecimal.ZERO
}

/** Набрано ли в поле число больше нуля. */
private fun positive(text: String): Boolean = (amount(text).value ?: BigDecimal.ZERO) > BigDecimal.ZERO
