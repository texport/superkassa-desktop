package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.button.FieldButton
import kz.mybrain.superkassa.designsystem.field.fieldMinWidth
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.address.AddressSearch
import kz.mybrain.superkassa.presentation.cabinet.mapPointOf
import kz.mybrain.superkassa.presentation.cabinet.places.placesViewModel
import kz.mybrain.superkassa.presentation.words.cabinet.addressIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
internal fun PlaceEditRow(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: RetailPlace,
    busy: Boolean
) {
    val model = placesViewModel(cabinet.cabinet)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.itemGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        PlaceRename(texts, place, busy) { model.rename(place, it) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PlaceMove(cabinet, texts, place, busy)
    }
}

/** Новое название точки. */
@Composable
private fun PlaceRename(texts: CabinetTexts, place: RetailPlace, busy: Boolean, onRename: (String) -> Unit) {
    var name by remember(place.id) { mutableStateOf(place.name) }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        itemVerticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(texts.placeName) },
            singleLine = true,
            // Поле тянется до кнопки: ряд занимает всю ширину карточки,
            // и края поля совпадают с краями полей над ним.
            modifier = Modifier.weight(1f).fieldMinWidth(texts.placeName, Sizes.fieldForm)
        )
        FieldButton(
            text = texts.rename,
            enabled = !busy && name.isNotBlank() && name != place.name
        ) { onRename(name) }
    }
}

/** Переезд точки: адрес из регистра и координаты нового места. */
@Composable
private fun PlaceMove(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: RetailPlace,
    busy: Boolean
) {
    val language = LocalLanguage.current
    val model = placesViewModel(cabinet.cabinet)
    val draft = remember(place.id) { MoveDraft(place) }
    val onAddress: (RegisterAddress) -> Unit = { draft.choose(it, language) }
    // Своё название у подраздела: над карточкой уже стоит строка «Адрес»
    // с нынешним адресом точки, и второй «Адрес» под ней читался как он же.
    AddressSearch(
        cabinet = cabinet,
        texts = texts,
        query = draft.query,
        onQuery = draft::type,
        owner = place.id,
        title = texts.changeAddress,
        onChoose = onAddress
    )
    if (draft.chosen != null) {
        Chip(texts.addressChosen, StatusColors.delivered)
    }
    // Пока новый адрес не выбран, карта открывается на нынешнем адресе точки:
    // переезжают обычно в соседний дом, а не в другой город.
    PlacePoint(cabinet, texts, draft.point, draft.here, onAddress) { draft.point = it }
    BusyButton(text = texts.changeAddress, busy = busy, enabled = draft.ready) {
        val address = draft.chosen ?: return@BusyButton
        val where = draft.point ?: return@BusyButton
        model.move(place, address, where, draft::clear)
    }
}

/**
 * Переезд точки, как его набирает владелец: адрес из регистра и место на карте.
 *
 * Со сменой адреса координаты снимаются: они принадлежали прежнему дому,
 * и переезд с чужими координатами — то самое расхождение, из-за которого
 * точка оказывалась в другом районе.
 */
internal class MoveDraft(private val place: RetailPlace) {
    var query by mutableStateOf("")
        private set
    var chosen by mutableStateOf<RegisterAddress?>(null)
        private set
    var point by mutableStateOf(mapPointOf(place.latitude, place.longitude))

    /** Адрес, на котором открывается карта: выбранный, а до выбора — нынешний адрес точки. */
    val here: RegisterAddress? get() = chosen ?: place.registerAddress()

    /** Переезд подаётся, когда выбраны и адрес, и место на карте. */
    val ready: Boolean get() = chosen != null && point != null

    /** Пустая подпись — адрес снят: выбранный раньше не должен уйти в кабинет. */
    fun type(entered: String) {
        query = entered
        if (entered.isBlank()) chosen = null
    }

    fun choose(address: RegisterAddress, language: Language) {
        if (address.addressRef != here?.addressRef) point = null
        chosen = address
        query = addressIn(language, address.address, address.addressKz)
    }

    /** Переезд состоялся: форма пуста. */
    fun clear() {
        chosen = null
        query = ""
    }
}

/** Нынешний адрес точки записью регистра: на нём открывается карта до выбора нового. */
private fun RetailPlace.registerAddress(): RegisterAddress? =
    addressRef?.let {
        RegisterAddress(addressRef = it, address = address, addressKz = addressKz, rka = rka, cato = cato)
    }
