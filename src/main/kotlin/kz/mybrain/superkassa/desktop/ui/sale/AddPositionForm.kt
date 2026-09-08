package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Ввод позиции чека руками.
 *
 * Поля стоят столбцом по ширине кассовой колонки, а не в одну длинную
 * строку: строка из шести полей на узкой колонке не помещается, и кассир
 * искал бы «Количество» за краем экрана.
 *
 * Enter добавляет позицию из любого поля формы: за кассой руки заняты
 * товаром, и тянуться к мыши ради каждой строки чека — потерянное время.
 * Кнопка недоступна ровно тогда, когда введённое ещё не образует позицию,
 * и строка над ней всегда называет, чего не хватает. Добавление —
 * второстепенное действие экрана, поэтому кнопка тональная: главное
 * действие здесь одно, и это «Пробить чек».
 */
@Composable
fun AddPositionForm(session: Session, onAdd: (Position) -> Unit) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val vat = defaultVatOf(session, LocalVatRates.current)
    var draft by remember(vat) { mutableStateOf(PositionDraft(vatGroup = vat)) }
    val submit: () -> Boolean = {
        val ready = draft.position
        if (ready != null) {
            onAdd(ready)
            draft = draft.cleared()
        }
        ready != null
    }

    Column(
        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
            event.type == KeyEventType.KeyDown && event.key in ENTER_KEYS && submit()
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        DraftFields(draft) { draft = it }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                enabled = draft.position != null,
                onClick = { submit() },
                modifier = Modifier.weight(1f)
            ) { Text(texts.sale.add, style = MaterialTheme.typography.titleSmall) }
        }
        Hint(draft.problems.firstOrNull()?.text(extra), extra.addByEnter)
    }
}

/** Поля позиции сверху вниз: что, почём, сколько, по какой ставке. */
@Composable
private fun DraftFields(draft: PositionDraft, onChange: (PositionDraft) -> Unit) {
    val texts = LocalStrings.current
    OutlinedTextField(
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
        label = { Text(texts.sale.name) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        DraftAmountField(draft, DraftField.Price, texts.sale.price) {
            onChange(draft.copy(price = it))
        }
        DraftAmountField(draft, DraftField.Quantity, texts.sale.quantity) {
            onChange(draft.copy(quantity = it))
        }
    }
    // Ставка и скидка стоят в одной строке, а не столбиком: форма позиции
    // целиком помещается в панель, и кассиру не приходится прокручивать её
    // посреди набора — прокрутка на фокусе уводила поле из-под курсора.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VatPicker(draft.vatGroup, Modifier.weight(1f)) { onChange(draft.copy(vatGroup = it)) }
        DraftAmountField(draft, DraftField.Discount, texts.sale.discount) {
            onChange(draft.copy(discount = it))
        }
    }
}

/**
 * Поле числа с подсветкой ошибки.
 *
 * Красным поле становится, только когда в нём что-то есть: пустая форма
 * при открытии смены не должна выглядеть набором ошибок.
 */
@Composable
private fun RowScope.DraftAmountField(
    draft: PositionDraft,
    field: DraftField,
    label: String,
    onChange: (String) -> Unit
) {
    val value = draft.valueOf(field)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = value.isNotBlank() && draft.problem(field) != null,
        modifier = Modifier.weight(1f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VatPicker(selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val texts = LocalStrings.current
    val rates = LocalVatRates.current
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = vatTitle(rates, selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(texts.sale.vat) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            rates.forEach { rate ->
                DropdownMenuItem(
                    text = { Text(rate.title) },
                    onClick = {
                        onSelect(rate.code)
                        open = false
                    }
                )
            }
        }
    }
}

/** Обычный Enter и Enter на цифровой части клавиатуры — одно и то же действие. */
private val ENTER_KEYS = setOf(Key.Enter, Key.NumPadEnter)
