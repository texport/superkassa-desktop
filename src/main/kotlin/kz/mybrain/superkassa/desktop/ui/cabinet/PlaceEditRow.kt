package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.ChangeAddressResult
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlaceAddress
import kz.mybrain.superkassa.desktop.server.cabinet.moveRetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.renameRetailPlace
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.map.MapPoint
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
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
    // Ряд переносится, а не сжимается: в узком окне поле заданной ширины
    // выдавливало кнопку, и «Переименовать» выходило как «Пере / имен».
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        itemVerticalAlignment = Alignment.Top
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
    var point by remember(place.id) {
        mutableStateOf(place.latitude?.let { lat -> place.longitude?.let { MapPoint(lat, it) } })
    }

    val onQuery: (String) -> Unit = { entered ->
        query = entered
        if (entered.isBlank()) chosen = null
    }
    // Со сменой адреса координаты снимаются: они принадлежали прежнему дому,
    // и переезд с чужими координатами — то самое расхождение, из-за которого
    // точка оказывалась в другом районе.
    val here = chosen ?: place.registerAddress()
    val onAddress: (RegisterAddress) -> Unit = { address ->
        if (address.addressRef != here?.addressRef) point = null
        chosen = address
        query = addressIn(session.language, address.address, address.addressKz)
    }
    // Своё название у подраздела: над карточкой уже стоит строка «Адрес»
    // с нынешним адресом точки, и второй «Адрес» под ней читался как он же.
    AddressSearch(
        session, cabinet, texts, query, onQuery,
        owner = place.id, title = texts.changeAddress, onChoose = onAddress
    )
    if (chosen != null) {
        Chip(texts.addressChosen, StatusColors.delivered)
    }
    // Пока новый адрес не выбран, карта открывается на нынешнем адресе точки:
    // переезжают обычно в соседний дом, а не в другой город.
    PlacePoint(session, cabinet, texts, point, here, onAddress) { point = it }
    BusyButton(
        text = texts.changeAddress,
        busy = cabinet.busy,
        enabled = chosen != null && point != null,
        onClick = {
            scope.launch {
                // Смена адреса удаётся не всегда: точку с кассами,
                // побывавшими в КГД, кабинет не переселяет и называет
                // те кассы, которые надо перерегистрировать. Оба ответа
                // приходят с HTTP 200, и различить их можно только здесь.
                val result = move(cabinet, place, chosen, point) ?: return@launch
                if (result.updated) {
                    chosen = null
                    query = ""
                    session.lastMessage = Message.Done(texts.addressChanged)
                    onChanged()
                } else {
                    session.lastMessage = Message.Refusal(blockedWords(texts, result), REREGISTRATION)
                }
            }
        }
    )
}

/** Нынешний адрес точки записью регистра: на нём открывается карта до выбора нового. */
private fun RetailPlace.registerAddress(): RegisterAddress? =
    addressRef?.let {
        RegisterAddress(addressRef = it, address = address, addressKz = addressKz, rka = rka, cato = cato)
    }

/** Переселяет точку. `null` означает отказ кабинета: выбранное остаётся на месте. */
private suspend fun move(
    cabinet: CabinetSession,
    place: RetailPlace,
    address: RegisterAddress?,
    point: MapPoint?
): ChangeAddressResult? {
    val token = cabinet.token ?: return null
    val chosen = address ?: return null
    val where = point ?: return null
    return cabinet.guard {
        cabinet.client.moveRetailPlace(
            token,
            place.id,
            RetailPlaceAddress(
                addressRef = chosen.addressRef,
                latitude = where.latitude,
                longitude = where.longitude
            )
        )
    }
}

/**
 * Почему адрес остался прежним — с перечнем касс.
 *
 * Без имён касс владельцу пришлось бы перебирать точку целиком: на ней
 * их бывает десяток, а мешают не все.
 */
private fun blockedWords(texts: CabinetTexts, result: ChangeAddressResult): String {
    val blocked = result.blockingCashRegisters.map { it.title() }.filter { it.isNotBlank() }
    return listOf(texts.addressNeedsReregistration, blocked.joinToString(", "))
        .filter { it.isNotBlank() }
        .joinToString(Glyphs.SEPARATOR)
}

/** Код помехи для поддержки: отказ по состоянию касс, а не ошибка запроса. */
private const val REREGISTRATION = "REREGISTRATION_REQUIRED"
