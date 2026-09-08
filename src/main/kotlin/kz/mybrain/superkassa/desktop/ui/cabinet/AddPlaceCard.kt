package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.width
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
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Заведение точки: название, адрес из регистра и координаты.
 *
 * Пока адрес не выбран из найденных, кнопка недоступна: без кода регистра
 * кабинет точку не примет, и отказ пришёл бы уже после нажатия. Поэтому же
 * выбранный адрес отмечен плашкой — по одному только заполненному полю
 * владелец не отличал набранное от выбранного.
 *
 * Карточка сворачивается и по умолчанию свёрнута: пять полей под списком
 * точек отжимали список наверх, а заводят точку редко. В мастере
 * подключения она открыта сразу — там завести точку и есть текущий шаг.
 *
 * @param opened открыта ли карточка при появлении.
 */
@Composable
fun AddPlaceCard(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    opened: Boolean = false,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<RegisterAddress?>(null) }
    // Координаты обязательны: кабинет отвергает точку без них, а в адресном
    // регистре их нет — их вводит владелец.
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(opened) }
    CollapsibleCard(
        title = texts.addPlace,
        expanded = expanded,
        onToggle = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(texts.placeName) },
            supportingText = { Text(texts.required) },
            singleLine = true,
            modifier = Modifier.width(Sizes.fieldForm)
        )
        AddressSearch(session, cabinet, texts, query, { query = it }) { address ->
            chosen = address
            query = addressIn(session.language, address.address, address.addressKz)
        }
        if (chosen != null) {
            Chip(texts.addressChosen, StatusColors.delivered)
        }
        PlaceCoordinates(texts, session.preferences, latitude, longitude, query, { latitude = it }, { longitude = it })
        BusyButton(
            text = texts.addPlace,
            busy = cabinet.busy,
            enabled = name.isNotBlank() && chosen != null && coordinatesReady(latitude, longitude),
            onClick = {
                scope.launch {
                    if (!create(cabinet, name, chosen, latitude, longitude)) return@launch
                    // Поля очищаются и список перечитывается только по удаче:
                    // иначе отказ стирался следующим же обращением, а введённое
                    // пропадало впустую.
                    name = ""
                    query = ""
                    chosen = null
                    latitude = ""
                    longitude = ""
                    onAdded()
                }
            }
        )
    }
}

/** Заводит точку в кабинете. Ложь означает отказ: введённое остаётся на месте. */
private suspend fun create(
    cabinet: CabinetSession,
    name: String,
    address: RegisterAddress?,
    latitude: String,
    longitude: String
): Boolean {
    val token = cabinet.token ?: return false
    val chosen = address ?: return false
    return cabinet.guard {
        cabinet.client.addRetailPlace(
            token,
            RetailPlaceCreate(
                name = name.trim(),
                addressRef = chosen.addressRef,
                latitude = degreesOf(latitude, MAX_LATITUDE),
                longitude = degreesOf(longitude, MAX_LONGITUDE)
            )
        )
    } != null
}
