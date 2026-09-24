package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.common.field.fieldMinWidth
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
 *
 * Поле тянется до кнопки, а на телефоне кнопка уходит под него: поле
 * постоянной ширины рядом с кнопкой выходило за край экрана.
 */
@Composable
fun OfdTokenCard(ofd: OfdSettingsUiState, actions: OfdSettingsActions) {
    val texts = LocalStrings.current.settings
    ofd.kkm ?: return
    SectionCard(title = texts.ofdToken, info = texts.tokenHint) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.inline),
            itemVerticalAlignment = Alignment.Top
        ) {
            TokenField(ofd, actions, Modifier.weight(1f))
            FieldButton(
                text = texts.saveToken,
                enabled = ofd.tokenEditable && ofd.token.isNotBlank(),
                onClick = actions::saveToken
            )
        }
    }
}

/** Поле нового токена: не уже своей подписи, дальше — сколько даёт строка. */
@Composable
private fun TokenField(ofd: OfdSettingsUiState, actions: OfdSettingsActions, modifier: Modifier) {
    val texts = LocalStrings.current.settings
    OutlinedTextField(
        value = ofd.token,
        onValueChange = actions::typeToken,
        label = { Text(texts.newToken) },
        singleLine = true,
        enabled = ofd.tokenEditable,
        modifier = modifier.fieldMinWidth(texts.newToken, Sizes.fieldChoice)
    )
}
