package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.common.field.fieldWidth
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Замена токена ОФД.
 *
 * Токен выдаёт ОФД, и он же его отзывает: по коду 2 «неверный токен» касса
 * встаёт и не выходит из блокировки, пока не введён новый. Без этого поля
 * заблокированную кассу с рабочего места было не вернуть в строй вовсе.
 */
@Composable
fun OfdTokenCard(ofd: OfdSettingsUiState, actions: OfdSettingsActions) {
    val texts = LocalStrings.current.settings
    ofd.kkm ?: return
    SectionCard(title = texts.ofdToken, info = texts.tokenHint) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug), verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = ofd.token,
                onValueChange = actions::typeToken,
                label = { Text(texts.newToken) },
                singleLine = true,
                enabled = ofd.tokenEditable,
                modifier = Modifier.fieldWidth(texts.newToken, Sizes.fieldChoice)
            )
            FieldButton(
                text = texts.saveToken,
                enabled = ofd.tokenEditable && ofd.token.isNotBlank(),
                onClick = actions::saveToken
            )
        }
    }
}
