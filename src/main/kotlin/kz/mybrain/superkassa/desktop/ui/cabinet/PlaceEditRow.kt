package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
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
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlaceAddress
import kz.mybrain.superkassa.desktop.server.cabinet.moveRetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.renameRetailPlace
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Правка торговой точки: название и адрес.
 *
 * Точка живёт дольше кассы: магазин переименовывают и переезжают, а её
 * адрес уходит в регистрационное заявление и в чек. Прежде кабинет умел
 * только завести и удалить, и переезд означал новую точку с переносом
 * на неё всех касс.
 *
 * Название и адрес меняются порознь — это разные действия и разные ручки
 * кабинета, — и разделены чертой: прежде четыре поля и три кнопки шли
 * подряд, и было не видно, что к чему относится.
 *
 * Переезд стал двумя шагами: сначала владелец выбирает адрес, потом
 * подтверждает переезд. Прежде выбор адреса переселял точку немедленно,
 * подставив в координаты нули, если поля были пусты.
 */
@Composable
fun PlaceEditRow(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    place: RetailPlace,
    onChanged: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        PlaceRename(cabinet, texts, place, onChanged)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PlaceMove(session, cabinet, texts, place, onChanged)
    }
}

/** Новое название точки. */
@Composable
private fun PlaceRename(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    place: RetailPlace,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(place.id) { mutableStateOf(place.name) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(texts.placeName) },
            singleLine = true,
            modifier = Modifier.width(Sizes.fieldForm)
        )
        FieldButton(
            text = texts.rename,
            enabled = !cabinet.busy && name.isNotBlank() && name != place.name
        ) {
            scope.launch {
                val token = cabinet.token ?: return@launch
                val renamed = cabinet.guard {
                    cabinet.client.renameRetailPlace(token, place.id, name.trim())
                }
                if (renamed != null) onChanged()
            }
        }
    }
}

/** Переезд точки: адрес из регистра и координаты нового места. */
@Composable
private fun PlaceMove(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    place: RetailPlace,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember(place.id) { mutableStateOf("") }
    var chosen by remember(place.id) { mutableStateOf<RegisterAddress?>(null) }
    var latitude by remember(place.id) { mutableStateOf(place.latitude?.toPlainString().orEmpty()) }
    var longitude by remember(place.id) { mutableStateOf(place.longitude?.toPlainString().orEmpty()) }

    AddressSearch(session, cabinet, texts, query, { query = it }) { address ->
        chosen = address
        query = addressIn(session.language, address.address, address.addressKz)
    }
    if (chosen != null) {
        Chip(texts.addressChosen, StatusColors.delivered)
    }
    PlaceCoordinates(texts, latitude, longitude, { latitude = it }, { longitude = it })
    BusyButton(
        text = texts.changeAddress,
        busy = cabinet.busy,
        enabled = chosen != null && coordinatesReady(latitude, longitude),
        onClick = {
            scope.launch {
                if (move(cabinet, place, chosen, latitude, longitude)) {
                    chosen = null
                    query = ""
                    onChanged()
                }
            }
        }
    )
}

/** Переселяет точку. Ложь означает отказ: выбранное остаётся на месте. */
private suspend fun move(
    cabinet: CabinetSession,
    place: RetailPlace,
    address: RegisterAddress?,
    latitude: String,
    longitude: String
): Boolean {
    val token = cabinet.token ?: return false
    val chosen = address ?: return false
    val moved = degreesOf(latitude, MAX_LATITUDE) ?: return false
    val meridian = degreesOf(longitude, MAX_LONGITUDE) ?: return false
    return cabinet.guard {
        cabinet.client.moveRetailPlace(
            token,
            place.id,
            RetailPlaceAddress(addressRef = chosen.addressRef, latitude = moved, longitude = meridian)
        )
    } != null
}
