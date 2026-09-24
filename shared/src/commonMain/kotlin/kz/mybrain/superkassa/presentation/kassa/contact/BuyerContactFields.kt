package kz.mybrain.superkassa.presentation.kassa.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kz.mybrain.superkassa.domain.kassa.model.BuyerContact
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.presentation.common.picker.WideChoiceSegments
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.kassa.BuyerContactTexts
import kz.mybrain.superkassa.presentation.strings.kassa.buyerContactTexts
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Куда отправить чек покупателю: не отправлять, телефон, почта или Telegram.
 *
 * Доставка необязательна, и выбор начинается с «не отправлять». Вид,
 * чей канал доставки не настроен, погашен и назван под рядом «не настроен».
 * Не настроено ничего — вместо выбора одна строка: чек покажут покупателю
 * или распечатают.
 *
 * Под полем контакта всегда одна строка: пока пусто — зачем поле, набрано
 * негодно — как набрать, разобрано — куда именно уйдёт чек. Последнее
 * кассир сверяет с покупателем вслух, до того как пробить.
 */
@Composable
fun BuyerContactFields(
    contact: BuyerContact,
    channels: ContactChannels,
    onKind: (ContactKind) -> Unit,
    onText: (String) -> Unit,
    enabled: Boolean = true
) {
    val texts = buyerContactTexts(LocalLanguage.current)
    if (channels.none) {
        Note(texts.unavailable)
        return
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        WideChoiceSegments(
            options = ContactKind.entries,
            selected = contact.kind,
            label = { texts.kinds.getValue(it) },
            enabled = enabled,
            available = channels::allows,
            onSelect = onKind
        )
        val off = ContactKind.entries.filterNot(channels::allows)
        if (off.isNotEmpty()) Note("${texts.notConfigured}: ${off.joinToString { texts.kinds.getValue(it) }}")
        if (contact.kind != ContactKind.None) ContactField(contact, texts, onText, enabled)
    }
}

@Composable
private fun ContactField(contact: BuyerContact, texts: BuyerContactTexts, onText: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = contact.text,
        onValueChange = onText,
        label = { Text(texts.labels.getValue(contact.kind)) },
        singleLine = true,
        enabled = enabled,
        isError = contact.malformed,
        supportingText = { Text(texts.below(contact)) },
        keyboardOptions = KeyboardOptions(keyboardType = contact.kind.keyboard, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Строка пояснения: мельче и тише поля, это не отказ. */
@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Строка под полем: зачем оно, как набрать или куда уйдёт чек. */
private fun BuyerContactTexts.below(contact: BuyerContact): String = when {
    contact.empty -> hint
    contact.malformed -> formats.getValue(contact.kind)
    else -> "$sendsTo ${contact.normalized}"
}

/** Клавиатура поля: телефон — номеронабирателем, почта — с «@», чат — цифрами. */
private val ContactKind.keyboard: KeyboardType
    get() = when (this) {
        ContactKind.Phone -> KeyboardType.Phone
        ContactKind.Email -> KeyboardType.Email
        ContactKind.Telegram, ContactKind.None -> KeyboardType.Number
    }
