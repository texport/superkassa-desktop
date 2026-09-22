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
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
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
 * не хватает, видно по самой форме — пустое название, нераскрытый адрес,
 * строка «Точка не выбрана», — и перечня под кнопкой нет: он повторял бы
 * названия полей второй раз. Так же устроены остальные формы кабинета,
 * см. `FormDialog`.
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
                val created = create(cabinet, name, chosen, point)
                if (created != null) {
                    cabinet.placeAdded(created)
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
        val onQuery: (String) -> Unit = { entered ->
            query = entered
            // Пустая подпись — адрес снят: выбранный раньше не должен уйти в кабинет
            if (entered.isBlank()) chosen = null
        }
        // Адрес выбирается или здесь, или в окне карты — тем же регистром
        // и в то же место: расходиться адресу точки и дому на карте нельзя.
        // Со сменой адреса координаты снимаются: они принадлежали прежнему дому.
        val onAddress: (RegisterAddress) -> Unit = { address ->
            if (address.addressRef != chosen?.addressRef) point = null
            chosen = address
            query = addressIn(session.language, address.address, address.addressKz)
        }
        AddressSearch(session, cabinet, texts, query, onQuery, onChoose = onAddress)
        PlacePoint(session, cabinet, texts, point, chosen, onAddress) { point = it }
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

/** Заводит точку в кабинете. `null` означает отказ: введённое остаётся на месте. */
private suspend fun create(
    cabinet: CabinetSession,
    name: String,
    address: RegisterAddress?,
    point: MapPoint?
): RetailPlace? {
    val token = cabinet.token ?: return null
    val chosen = address ?: return null
    val where = point ?: return null
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
    }
}
