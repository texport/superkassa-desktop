package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterEdit
import kz.mybrain.superkassa.desktop.server.cabinet.editRegister
import kz.mybrain.superkassa.desktop.server.cabinet.removeRegister
import kz.mybrain.superkassa.desktop.server.cabinet.renameRegister
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
fun RegisterEditCard(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val draft = register.registrationNumber.isNullOrBlank()

    EditRow(
        label = texts.internalName,
        save = texts.save,
        hint = texts.hints.internalName,
        initial = register.internalName.orEmpty(),
        key = register.id,
        enabled = true,
        allowBlank = true,
        busy = cabinet.busy
    ) { value ->
        scope.launch { if (rename(cabinet, register.id, value)) onChanged() }
    }
    EditRow(
        label = texts.factoryNumber,
        save = texts.save,
        hint = if (draft) texts.hints.factoryNumber else texts.factoryLocked,
        initial = register.factoryNumber.orEmpty(),
        key = register.id,
        enabled = draft,
        busy = cabinet.busy
    ) { value ->
        scope.launch { if (restamp(cabinet, register.id, value)) onChanged() }
    }
    RegisterRemoval(cabinet, texts, register, draft, onChanged)
}

/**
 * Удаление кассы и объяснение, когда его нет.
 *
 * Кнопка с обводкой, а не текстовая: она стоит последней в разделе,
 * и текстовой её путали с подписью под полем.
 */
@Composable
private fun RegisterRemoval(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    draft: Boolean,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    OutlinedButton(
        enabled = !cabinet.busy && draft,
        onClick = {
            scope.launch {
                val token = cabinet.token ?: return@launch
                cabinet.guard { cabinet.client.removeRegister(token, register.id) }
                cabinet.refreshRegisters()
                onChanged()
            }
        }
    ) { Text(texts.deleteRegister) }
    if (!draft) {
        Text(
            text = texts.deleteOnlyDraft,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Записывает своё название владельца. Пустое имя стирает заметку. */
private suspend fun rename(cabinet: CabinetSession, id: String, value: String): Boolean {
    val token = cabinet.token ?: return false
    return cabinet.guard {
        cabinet.client.renameRegister(token, id, value.takeIf { it.isNotBlank() })
    } != null
}

/** Записывает заводской номер: только пока касса не на учёте. */
private suspend fun restamp(cabinet: CabinetSession, id: String, value: String): Boolean {
    val token = cabinet.token ?: return false
    return cabinet.guard {
        cabinet.client.editRegister(token, id, RegisterEdit(factoryNumber = value))
    } != null
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
 * @param hint что это за поле, а у погашенного — почему его не правят.
 */
@Composable
private fun EditRow(
    label: String,
    save: String,
    hint: String,
    initial: String,
    key: String,
    enabled: Boolean,
    busy: Boolean,
    allowBlank: Boolean = false,
    onSave: (String) -> Unit
) {
    var value by remember(key) { mutableStateOf(initial) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(label) },
            trailingIcon = { InfoTip(hint) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.width(Sizes.fieldForm)
        )
        val changed = value.trim() != initial && (allowBlank || value.isNotBlank())
        FieldButton(text = save, enabled = enabled && !busy && changed) { onSave(value.trim()) }
    }
}
