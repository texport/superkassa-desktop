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
 * Данные покупателя: его ИИН или БИН и реквизиты выбранной отрасли.
 *
 * Стоят внизу кассовой колонки намеренно и свёрнуты по умолчанию:
 * заполняются они редко, а штрихкод, оплата и итог нужны в каждом чеке.
 *
 * Отраслевые поля стоят здесь же: номер счёта, номер карты и номер машины
 * принадлежат тому, кому выписан чек, и спрашивают их у того же человека,
 * что и ИИН.
 */
@Composable
fun CustomerDataCard(form: SaleForm, expanded: Boolean, onToggle: () -> Unit) {
    val extra = LocalSaleTexts.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CollapsibleSection(
                title = extra.customerData,
                expanded = expanded,
                onToggle = onToggle
            ) {
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
