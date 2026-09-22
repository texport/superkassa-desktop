package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
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
    var address by remember { mutableStateOf(session.preferences.nodeUrl) }
    SectionCard(title = texts.nodeAddress, info = texts.nodeAddressHint) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(texts.nodeAddress) },
                singleLine = true,
                modifier = Modifier.width(Sizes.fieldName)
            )
            FilledTonalButton(
                modifier = Modifier.height(Sizes.fieldHeight),
                enabled = address.isNotBlank() && address.trim() != session.preferences.nodeUrl,
                // Пробел по краям адреса приходит из буфера обмена вместе
                // со скопированной строкой, а узел по такому адресу не ищется.
                onClick = { session.preferences.nodeUrl = address.trim() }
            ) { Text(texts.save) }
        }
    }
}
