package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.dialog.FormDialog
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.address.AddressSearch
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlacePoint
import kz.mybrain.superkassa.presentation.common.mapview.MapPoint
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.words.cabinet.addressIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
internal fun AddPlaceCard(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val model = placesViewModel(cabinet.cabinet)
    val window by cabinet.cabinet.state.collectAsScreenState()
    val draft = remember { NewPlaceDraft() }
    FormDialog(
        title = texts.places.add,
        icon = AppIcons.newKkm,
        action = texts.places.add,
        close = texts.close,
        busy = window.busy,
        missing = missingFields(texts, draft.name, draft.chosen, draft.point),
        onDismiss = onDismiss,
        onAction = { draft.add(model) { onAdded().also { onDismiss() } } }
    ) {
        NewPlaceFields(cabinet, texts, draft)
    }
}

/**
 * Поля новой точки: название, адрес и место.
 *
 * Адрес выбирается или здесь, или в окне карты — тем же регистром и в то же
 * место: расходиться адресу точки и дому на карте нельзя.
 */
@Composable
private fun NewPlaceFields(cabinet: CabinetWindow, texts: CabinetTexts, draft: NewPlaceDraft) {
    val language = LocalLanguage.current
    OutlinedTextField(
        value = draft.name,
        onValueChange = { draft.name = it },
        label = { Text(texts.places.name) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    val onAddress: (RegisterAddress) -> Unit = { draft.choose(it, language) }
    AddressSearch(cabinet, texts, draft.query, draft::type, onChoose = onAddress)
    PlacePoint(cabinet, texts, draft.point, draft.chosen, onAddress) { draft.point = it }
}

/**
 * Новая точка, как её набирает владелец: название, адрес из регистра и место на карте.
 *
 * Со сменой адреса координаты снимаются: они принадлежали прежнему дому.
 */
internal class NewPlaceDraft {
    var name by mutableStateOf("")
    var query by mutableStateOf("")
        private set
    var chosen by mutableStateOf<RegisterAddress?>(null)
        private set
    var point by mutableStateOf<MapPoint?>(null)

    /** Пустая подпись — адрес снят: выбранный раньше не должен уйти в кабинет. */
    fun type(entered: String) {
        query = entered
        if (entered.isBlank()) chosen = null
    }

    fun choose(address: RegisterAddress, language: Language) {
        if (address.addressRef != chosen?.addressRef) point = null
        chosen = address
        query = addressIn(language, address.address, address.addressKz)
    }

    /** Заводит точку, если адрес и место выбраны; иначе ничего не делает. */
    fun add(model: PlacesViewModel, onAdded: () -> Unit) {
        val address = chosen ?: return
        val where = point ?: return
        model.add(name, address, where) { onAdded() }
    }
}

/** Чего не хватает, чтобы кабинет принял точку. */
private fun missingFields(
    texts: CabinetTexts,
    name: String,
    address: RegisterAddress?,
    point: MapPoint?
): List<String> = listOfNotNull(
    texts.places.name.takeIf { name.isBlank() },
    texts.places.address.takeIf { address == null },
    texts.places.pickOnMap.takeIf { point == null }
)
