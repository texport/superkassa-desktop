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
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Адрес личного кабинета ОФД.
 *
 * Кабинет — отдельная служба: на этой машине он занимает соседний порт,
 * на стенде стоит своим адресом. Новый адрес берётся при следующем входе
 * в раздел кабинета, а не на лету: менять адрес под открытым сеансом
 * значит показывать чужие данные под прежним именем.
 */
@Composable
fun CabinetAddressCard(session: Session) {
    val texts = cabinetTexts(session.language)
    val settings = LocalStrings.current.settings
    // Набранное переживает уход в другой раздел: экран настроек уходит
    // из состава вместе с ним, и поле забывало набранное молча.
    val address = SettingsDrafts.of(SettingsDrafts.Field.CABINET_ADDRESS, session.preferences.cabinetUrl)
    // Негодный адрес назван до сохранения: по адресу без схемы входа
    // в кабинет не будет вовсе, а на экране это выходило как «кабинет
    // не отвечает по заданному адресу».
    val malformed = address.isNotBlank() && !ServiceAddress.valid(address)
    SectionCard(title = texts.address, info = texts.hints.address) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { SettingsDrafts.type(SettingsDrafts.Field.CABINET_ADDRESS, it) },
                label = { Text(texts.address) },
                isError = malformed,
                supportingText = if (malformed) ({ Text(settings.addressMalformed) }) else null,
                singleLine = true,
                modifier = Modifier.width(Sizes.fieldName)
            )
            FilledTonalButton(
                modifier = Modifier.height(Sizes.fieldHeight),
                enabled = ServiceAddress.changed(address, session.preferences.cabinetUrl),
                onClick = {
                    session.preferences.cabinetUrl = ServiceAddress.tidy(address)
                    SettingsDrafts.forget(SettingsDrafts.Field.CABINET_ADDRESS)
                }
            ) { Text(texts.save) }
        }
    }
}
