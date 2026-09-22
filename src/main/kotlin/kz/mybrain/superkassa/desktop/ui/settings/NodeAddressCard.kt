package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Адрес узла, с которым работает касса.
 *
 * Узел обычно стоит на этой же машине, но за прилавком бывает иначе —
 * один узел на несколько рабочих мест. Прежде адрес был зашит, и такой
 * кассе оставалось только не работать. Адрес читается при каждом
 * обращении, так что новый действует сразу, без перезапуска.
 */
@Composable
fun NodeAddressCard(session: Session) {
    val texts = LocalStrings.current.settings
    // Набранное переживает уход в другой раздел: экран настроек уходит
    // из состава вместе с ним, и поле забывало набранное молча.
    val address = SettingsDrafts.of(SettingsDrafts.Field.NODE_ADDRESS, session.preferences.nodeUrl)
    // Негодный адрес назван до сохранения: по адресу без схемы обращения
    // не будет вовсе, а на экране это выходило как «узел недоступен» —
    // и владелец шёл искать сеть вместо своей опечатки.
    val malformed = address.isNotBlank() && !ServiceAddress.valid(address)
    SectionCard(title = texts.nodeAddress, info = texts.nodeAddressHint) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { SettingsDrafts.type(SettingsDrafts.Field.NODE_ADDRESS, it) },
                label = { Text(texts.nodeAddress) },
                isError = malformed,
                supportingText = if (malformed) ({ Text(texts.addressMalformed) }) else null,
                singleLine = true,
                modifier = Modifier.width(Sizes.fieldName)
            )
            FilledTonalButton(
                modifier = Modifier.height(Sizes.fieldHeight),
                enabled = ServiceAddress.changed(address, session.preferences.nodeUrl),
                onClick = {
                    session.preferences.nodeUrl = ServiceAddress.tidy(address)
                    SettingsDrafts.forget(SettingsDrafts.Field.NODE_ADDRESS)
                }
            ) { Text(texts.save) }
        }
    }
}
