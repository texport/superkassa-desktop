package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
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
    var address by remember { mutableStateOf(session.preferences.cabinetUrl) }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Text(texts.address, style = MaterialTheme.typography.titleMedium)
            Text(
                text = texts.addressHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(texts.address) },
                    singleLine = true,
                    modifier = Modifier.width(Sizes.fieldName)
                )
                FilledTonalButton(
                    modifier = Modifier.height(Sizes.fieldHeight),
                    enabled = address.isNotBlank() && address != session.preferences.cabinetUrl,
                    onClick = { session.preferences.cabinetUrl = address }
                ) { Text(texts.save) }
            }
        }
    }
}
