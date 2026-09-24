package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.common.field.fieldMinWidth
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Правка кассы в кабинете: своё название, заводской номер и удаление.
 *
 * Своё название меняется всегда — это заметка владельца, в ОФД она
 * не уходит. Заводской номер правится, пока касса не поставлена на учёт:
 * после постановки он записан в регистрационной карте, и менять его
 * можно только перерегистрацией.
 *
 * Удаление тоже только до учёта: снятая с учёта касса остаётся в истории,
 * а поставленную удаляют заявлением, а не кнопкой. Кнопка удаления —
 * с обводкой, а не текстовая: она стоит последней в разделе, и текстовой
 * её путали с подписью под полем.
 */
@Composable
fun RegisterEditCard(texts: CabinetTexts, register: CabinetRegister, busy: Boolean, model: RegisterViewModel) {
    val draft = editableInCabinet(register)
    NameRow(texts, register, busy, model::rename)
    EditRow(
        field = EditField(
            label = texts.factoryNumber,
            hint = if (draft) texts.hints.factoryNumber else texts.factoryLocked,
            initial = register.factoryNumber.orEmpty()
        ),
        save = texts.save,
        key = register.id,
        enabled = draft,
        busy = busy,
        onSave = model::restamp
    )
    RegisterRemoval(texts, draft, busy, model::remove)
}

/** Своё название владельца: меняется всегда; стереть его кабинет не даёт. */
@Composable
private fun NameRow(texts: CabinetTexts, register: CabinetRegister, busy: Boolean, onSave: (String) -> Unit) {
    EditRow(
        field = EditField(texts.internalName, texts.hints.internalName, register.internalName.orEmpty()),
        save = texts.save,
        key = register.id,
        enabled = true,
        busy = busy,
        onSave = onSave
    )
}

/**
 * Удаление кассы и объяснение, когда его нет.
 *
 * Кнопка с обводкой, а не текстовая: она стоит последней в разделе,
 * и текстовой её путали с подписью под полем.
 */
@Composable
private fun RegisterRemoval(texts: CabinetTexts, draft: Boolean, busy: Boolean, onRemove: () -> Unit) {
    OutlinedButton(enabled = !busy && draft, onClick = onRemove) { Text(texts.deleteRegister) }
    if (!draft) {
        Text(
            text = texts.deleteOnlyDraft,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Поле правки с кнопкой сохранения рядом.
 *
 * Кнопка загорается только когда значение отличается от записанного:
 * сохранение того же самого — обращение к кабинету впустую, а владельцу
 * оно выглядит как будто правка не применилась.
 *
 * Объяснение поля — под значком в самом поле, а не строкой под ним:
 * строкой оно занимало бы три строки высоты у каждого из полей всегда,
 * а читают его один раз.
 *
 * @param field что правится и что записано сейчас.
 */
@Composable
private fun EditRow(
    field: EditField,
    save: String,
    key: String,
    enabled: Boolean,
    busy: Boolean,
    onSave: (String) -> Unit
) {
    var value by remember(key) { mutableStateOf(field.initial) }
    // Кнопка переносится под поле, а не сжимается рядом с ним: поле
    // стоит фиксированной ширины, и в узкой колонке кнопке оставалось
    // столько, что «Сохранить» разрывалось на «Сохрани» и «ть».
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline),
        itemVerticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(field.label) },
            trailingIcon = { InfoTip(field.hint) },
            singleLine = true,
            enabled = enabled,
            // Поле тянется до кнопки: края полей карточки совпадают.
            modifier = Modifier.weight(1f).fieldMinWidth(field.label, Sizes.fieldForm)
        )
        val changed = value.trim() != field.initial && value.isNotBlank()
        FieldButton(text = save, enabled = enabled && !busy && changed) { onSave(value.trim()) }
    }
}

/**
 * Поле правки кассы.
 *
 * @property hint что это за поле, а у погашенного — почему его не правят.
 * @property initial что записано в кабинете сейчас.
 */
private class EditField(val label: String, val hint: String, val initial: String)
