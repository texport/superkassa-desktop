package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.server.cabinet.addresses
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Поиск адреса в государственном регистре.
 *
 * Один и тот же на заведение точки и на её переезд: это одно действие
 * владельца, и написанное дважды оно разошлось бы — в заведении найденное
 * уже показывалось строками, а в переезде подписью «Сменить адрес: …»
 * поперёк всей карточки.
 *
 * Найденное показано теми же строками, что и остальные списки кабинета:
 * стопка текстовых кнопок не давала понять, где кончается один адрес
 * и начинается следующий.
 *
 * @param query что набрал владелец; хранится снаружи, потому что при
 *   выборе адреса поле заполняется его названием.
 * @param onChoose выбранный адрес; список найденного после этого гаснет.
 */
@Composable
fun AddressSearch(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    query: String,
    onQuery: (String) -> Unit,
    onChoose: (RegisterAddress) -> Unit
) {
    val scope = rememberCoroutineScope()
    var found by remember { mutableStateOf<List<RegisterAddress>>(emptyList()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text(texts.placeAddress) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FieldButton(text = texts.findAddress, enabled = query.isNotBlank()) {
                scope.launch {
                    val token = cabinet.token ?: return@launch
                    found = cabinet.guard { cabinet.client.addresses(token, query) }?.items.orEmpty()
                }
            }
        }
        if (found.isEmpty()) return@Column
        Text(
            text = texts.foundAddresses,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        found.forEachIndexed { at, address ->
            RecordRow(
                title = addressIn(session.language, address.address, address.addressKz),
                subtitle = address.rka,
                striped = at % STRIPE == 1,
                onClick = {
                    onChoose(address)
                    found = emptyList()
                }
            )
        }
    }
}

/** Затеняется каждая вторая строка списка. */
private const val STRIPE = 2
