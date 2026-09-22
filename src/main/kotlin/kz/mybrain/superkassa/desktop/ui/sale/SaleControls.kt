package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleSection
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Заголовок чека: направление операции и очистка набранного.
 *
 * Направление — сегментами: их ровно два, оба видны сразу, и выбранное
 * читается без открывания списка. Очистка — второстепенное действие
 * и потому текстовой кнопкой; появляется только когда есть что очищать.
 */
@Composable
fun SaleHeader(form: SaleForm, basket: Basket) {
    val texts = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texts.sale.receipt,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f)
        )
        OperationChoice(form)
        if (basket.positions.isNotEmpty()) {
            TextButton(onClick = { basket.clear() }) { Text(texts.sale.clearBasket) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationChoice(form: SaleForm) {
    val texts = LocalStrings.current
    ChoiceSegments(
        options = SaleOperation.entries,
        selected = form.operation,
        label = { it.title(texts.sale) }
    ) { form.operation = it }
}

/**
 * Реквизиты чека: скидка и наценка на весь чек, покупатель и отрасль.
 *
 * Стоят внизу кассовой колонки намеренно и свёрнуты по умолчанию:
 * заполняются они редко, а штрихкод, оплата и итог нужны в каждом чеке.
 *
 * Скидка на чек краснеет, когда в корзине уже есть скидка на позицию:
 * узел отвечает на такой чек RECEIPT_DISCOUNT_SCOPES_CONFLICT, и узнать
 * об этом кассир должен здесь, а не после нажатия.
 */
@Composable
fun ReceiptDetailsCard(form: SaleForm, basket: Basket, expanded: Boolean, onToggle: () -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val conflict = basket.hasItemDiscount &&
        (amount(form.discount).value ?: BigDecimal.ZERO) > BigDecimal.ZERO
    // Минус в скидке и в наценке меняет их смысл на обратный: поле
    // краснеет у того, где он набран, а причина стоит строкой под обоими.
    val discountBelowZero = below(form.discount)
    val markupBelowZero = below(form.markup)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CollapsibleSection(
                title = extra.receiptDetails,
                expanded = expanded,
                onToggle = onToggle
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug)) {
                    OutlinedTextField(
                        value = form.discount,
                        onValueChange = form::enterDiscount,
                        label = { Text(texts.sale.receiptDiscount) },
                        singleLine = true,
                        isError = conflict || discountBelowZero,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = form.markup,
                        onValueChange = form::enterMarkup,
                        label = { Text(texts.sale.receiptMarkup) },
                        singleLine = true,
                        isError = markupBelowZero,
                        modifier = Modifier.weight(1f)
                    )
                }
                Hint(
                    problem = when {
                        discountBelowZero || markupBelowZero -> extra.blockDiscountNegative
                        conflict -> extra.blockDiscountScopes
                        else -> null
                    },
                    hint = texts.sale.discountOrMarkup
                )
                CustomerBinField(form)
                DomainPanel(form.domain) { form.domain = it }
            }
        }
    }
}

/** ИИН/БИН необязателен, но введённый — ровно двенадцать цифр. */
@Composable
private fun CustomerBinField(form: SaleForm) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    OutlinedTextField(
        value = form.customerBin,
        onValueChange = { form.customerBin = it.filter(Char::isDigit).take(BIN_LENGTH) },
        label = { Text(texts.sale.customerBin) },
        singleLine = true,
        isError = !binAccepted(form.customerBin),
        supportingText = {
            Text(
                if (form.customerBin.isEmpty()) {
                    extra.binHint
                } else {
                    "${form.customerBin.length} / $BIN_LENGTH"
                }
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** Набрано ли в поле число меньше нуля: пустое и недобранное — не минус. */
private fun below(text: String): Boolean {
    val value = amount(text).value ?: return false
    return value < BigDecimal.ZERO
}
