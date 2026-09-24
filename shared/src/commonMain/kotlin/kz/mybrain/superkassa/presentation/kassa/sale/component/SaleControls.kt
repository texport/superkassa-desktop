package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kz.mybrain.superkassa.designsystem.picker.ChoiceSegments
import kz.mybrain.superkassa.designsystem.section.CollapsibleSection
import kz.mybrain.superkassa.designsystem.section.ScreenTitle
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.sale.BIN_LENGTH
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleOperation
import kz.mybrain.superkassa.domain.kassa.model.sale.binAccepted
import kz.mybrain.superkassa.presentation.kassa.contact.BuyerContactFields
import kz.mybrain.superkassa.presentation.kassa.sale.FormActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleActions
import kz.mybrain.superkassa.presentation.words.kassa.title

/**
 * Заголовок чека: направление операции и очистка набранного.
 *
 * Направление — сегментами: их ровно два, оба видны сразу, и выбранное
 * читается без открывания списка. Очистка — второстепенное действие
 * и потому текстовой кнопкой; появляется только когда есть что очищать.
 *
 * Ряд переносится, а не сжимается. Ширина сегментов задана их подписями,
 * и в окне шириной в тысячу точек листу чека остаётся четверть ширины:
 * прежде заголовок отдавал её сегментам и рассыпался столбиком по одной
 * букве. Теперь при нехватке места направление и очистка уходят на
 * вторую строку, а название остаётся названием — общим заголовком экрана,
 * который сам сокращается многоточием, если места нет и под него.
 */
@Composable
fun SaleHeader(operation: SaleOperation, filled: Boolean, actions: SaleActions) {
    val texts = LocalStrings.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        ScreenTitle(texts.sale.receipt, Modifier.weight(1f, fill = false))
        OperationChoice(operation, actions.form::operation)
        if (filled) {
            TextButton(onClick = actions.basket::clear) { Text(texts.sale.clearBasket) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationChoice(operation: SaleOperation, onSelect: (SaleOperation) -> Unit) {
    val texts = LocalStrings.current
    ChoiceSegments(
        options = SaleOperation.entries,
        selected = operation,
        label = { it.title(texts.sale) },
        onSelect = onSelect
    )
}

/**
 * Данные покупателя: контакт, по которому ему уходит чек, и ИИН или БИН.
 *
 * Стоят внизу кассовой колонки намеренно и свёрнуты по умолчанию:
 * заполняются они не в каждом чеке, а штрихкод, оплата и итог нужны всегда.
 */
@Composable
fun CustomerDataCard(
    form: SaleForm,
    channels: ContactChannels,
    actions: FormActions,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val extra = LocalSaleTexts.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
        ) {
            CollapsibleSection(
                title = extra.customerData,
                expanded = expanded,
                onToggle = onToggle
            ) {
                BuyerContactFields(form.contact, channels, actions.contact::kind, actions.contact::text)
                CustomerBinField(form.customerBin, actions::customerBin)
            }
        }
    }
}

/** ИИН/БИН необязателен, но введённый — ровно двенадцать цифр. */
@Composable
private fun CustomerBinField(bin: String, onBin: (String) -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    OutlinedTextField(
        value = bin,
        onValueChange = onBin,
        label = { Text(texts.sale.customerBin) },
        singleLine = true,
        // ИИН и БИН — двенадцать цифр: клавиатура цифровая.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = !binAccepted(bin),
        supportingText = { Text(if (bin.isEmpty()) extra.binHint else "${bin.length} / $BIN_LENGTH") },
        modifier = Modifier.fillMaxWidth()
    )
}
