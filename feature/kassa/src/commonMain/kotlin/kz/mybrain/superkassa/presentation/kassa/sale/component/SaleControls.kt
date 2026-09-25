package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kz.mybrain.superkassa.designsystem.picker.ChoiceSegments
import kz.mybrain.superkassa.designsystem.section.CollapsibleSection
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
 * Шапка листа чека: направление операции, сколько позиций набрано
 * и очистка набранного.
 *
 * Стоит в самой карточке чека, а не рядом над ней: отдельный ряд
 * с надписью «Чек» над карточкой опускал лист ниже кассы справа, и две
 * колонки экрана начинались на разной высоте. Название раздела и так
 * в шапке окна.
 *
 * Направление — сегментами: их ровно два, оба видны сразу, и выбранное
 * читается без открывания списка. Очистка — второстепенное действие
 * и потому текстовой кнопкой; появляется только когда есть что очищать.
 * Сумма позиций стоит в денежном блоке справа и здесь не повторяется:
 * два одинаковых числа на одном экране кассир сверяет.
 *
 * Ряд переносится, а не сжимается: в узкой колонке счёт и очистка уходят
 * на вторую строку, а не рассыпаются столбиком по одной букве.
 */
@Composable
fun SaleHeader(operation: SaleOperation, count: Int, actions: SaleActions) {
    val texts = LocalStrings.current
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.cardPadding, vertical = Spacing.fieldGap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        OperationChoice(operation, actions.form::operation)
        Text(
            text = "${LocalSaleTexts.current.positionsCount}: $count",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            TextButton(onClick = actions.basket::clear) { Text(texts.receipt.clearBasket) }
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
        label = { it.title(texts.receipt) },
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
internal fun CustomerDataCard(
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
        label = { Text(texts.receipt.customerBin) },
        singleLine = true,
        // ИИН и БИН — двенадцать цифр: клавиатура цифровая.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = !binAccepted(bin),
        supportingText = { Text(if (bin.isEmpty()) extra.binHint else "${bin.length} / $BIN_LENGTH") },
        modifier = Modifier.fillMaxWidth()
    )
}
