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
    // Набранное переживает уход в другой раздел: экран настроек уходит
    // из состава вместе с ним, и поле забывало набранное молча.
    val address = SettingsDrafts.of(SettingsDrafts.Field.CABINET_ADDRESS, session.preferences.cabinetUrl)
    SectionCard(title = texts.address, info = texts.hints.address) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { SettingsDrafts.type(SettingsDrafts.Field.CABINET_ADDRESS, it) },
                label = { Text(texts.address) },
                singleLine = true,
                modifier = Modifier.width(Sizes.fieldName)
            )
            FilledTonalButton(
                modifier = Modifier.height(Sizes.fieldHeight),
                enabled = address.isNotBlank() && address.trim() != session.preferences.cabinetUrl,
                // Пробел по краям адреса приходит из буфера обмена вместе
                // со скопированной строкой, а кабинет по такому адресу не ищется.
                onClick = {
                    session.preferences.cabinetUrl = address.trim()
                    SettingsDrafts.forget(SettingsDrafts.Field.CABINET_ADDRESS)
                }
            ) { Text(texts.save) }
        }
    }
}
