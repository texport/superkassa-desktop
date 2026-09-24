package kz.mybrain.superkassa.presentation.kassa.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kz.mybrain.superkassa.domain.kassa.model.BuyerContact
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.presentation.common.picker.WideChoiceSegments
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.kassa.BuyerContactTexts
import kz.mybrain.superkassa.presentation.strings.kassa.buyerContactTexts
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Контакт покупателя: вид и сам контакт — по нему ему уходит чек.
 *
 * Под полем всегда одна строка: пока пусто — зачем поле, набрано негодно —
 * как набрать, разобрано — куда именно уйдёт чек. Последнее кассир сверяет
 * с покупателем вслух, до того как пробить.
 */
@Composable
fun BuyerContactFields(
    contact: BuyerContact,
    onKind: (ContactKind) -> Unit,
    onText: (String) -> Unit,
    enabled: Boolean = true
) {
    val texts = buyerContactTexts(LocalLanguage.current)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        WideChoiceSegments(
            options = ContactKind.entries,
            selected = contact.kind,
            label = { texts.kinds.getValue(it) },
            enabled = enabled,
            onSelect = onKind
        )
        OutlinedTextField(
            value = contact.text,
            onValueChange = onText,
            label = { Text(texts.labels.getValue(contact.kind)) },
            singleLine = true,
            enabled = enabled,
            isError = contact.malformed,
            supportingText = { Text(texts.below(contact)) },
            keyboardOptions = KeyboardOptions(keyboardType = contact.kind.keyboard),
            modifier = Modifier.fillMaxWidth()
        )
    }
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
        ContactKind.Telegram -> KeyboardType.Number
    }
