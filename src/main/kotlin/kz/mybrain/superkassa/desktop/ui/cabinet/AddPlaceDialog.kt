package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlaceCreate
import kz.mybrain.superkassa.desktop.server.cabinet.addRetailPlace
import kz.mybrain.superkassa.desktop.ui.components.FormDialog
import kz.mybrain.superkassa.desktop.ui.map.MapPoint
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Создание торговой точки: название, адрес из регистра и место на карте.
 *
 * Окном, а не карточкой под списком: точку создают раз в жизни, а список
 * смотрят каждый день, и форма отжимала его вниз.
 *
 * Пока адрес не выбран из найденных, кнопка недоступна: без кода регистра
 * кабинет точку не примет, и отказ пришёл бы уже после нажатия. Чего
 * не хватает — сказано под кнопкой, а не подписью «Обязательно» под
 * каждым полем.
 */
@Composable
fun AddPlaceCard(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<RegisterAddress?>(null) }
    var point by remember { mutableStateOf<MapPoint?>(null) }

    val missing = missingFields(texts, name, chosen, point)
    FormDialog(
        title = texts.addPlace,
        icon = AppIcons.newKkm,
        action = texts.addPlace,
        close = texts.close,
        busy = cabinet.busy,
        missing = missing,
        onDismiss = onDismiss,
        onAction = {
            scope.launch {
                if (create(cabinet, name, chosen, point)) {
                    onAdded()
                    onDismiss()
                }
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(texts.placeName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        AddressSearch(session, cabinet, texts, query, { query = it }) { address ->
            chosen = address
            query = addressIn(session.language, address.address, address.addressKz)
        }
        PlacePoint(texts, session.preferences, point, query) { point = it }
    }
}

/** Чего не хватает, чтобы кабинет принял точку. */
private fun missingFields(
    texts: CabinetTexts,
    name: String,
    address: RegisterAddress?,
    point: MapPoint?
): List<String> = listOfNotNull(
    texts.placeName.takeIf { name.isBlank() },
    texts.placeAddress.takeIf { address == null },
    texts.pickOnMap.takeIf { point == null }
)

/** Заводит точку в кабинете. Ложь означает отказ: введённое остаётся на месте. */
private suspend fun create(
    cabinet: CabinetSession,
    name: String,
    address: RegisterAddress?,
    point: MapPoint?
): Boolean {
    val token = cabinet.token ?: return false
    val chosen = address ?: return false
    val where = point ?: return false
    return cabinet.guard {
        cabinet.client.addRetailPlace(
            token,
            RetailPlaceCreate(
                name = name.trim(),
                addressRef = chosen.addressRef,
                latitude = where.latitude,
                longitude = where.longitude
            )
        )
    } != null
}
