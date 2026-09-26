package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.dialog.ConfirmDangerDialog
import kz.mybrain.superkassa.designsystem.dialog.FormDialog
import kz.mybrain.superkassa.designsystem.section.DetailLine
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.address.AddressSearch
import kz.mybrain.superkassa.presentation.cabinet.places.placesViewModel
import kz.mybrain.superkassa.presentation.words.cabinet.addressIn
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.fill

/**
 * Окно правки точки, открытое кнопкой карточки.
 *
 * Удаление необратимо и спрашивается окном предупреждения с красной
 * кнопкой; переименование и переезд — формами.
 *
 * @param onClose окно закрыто — отменой или сделанным делом.
 */
@Composable
internal fun PlaceEditDialog(
    edit: PlaceEdit,
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: RetailPlace,
    busy: Boolean,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val model = placesViewModel(cabinet.cabinet)
    when (edit) {
        PlaceEdit.Rename -> RenameDialog(texts, place, busy, onClose) { name ->
            model.rename(place, name)
            onClose()
        }
        PlaceEdit.Move -> MoveDialog(cabinet, texts, place, busy, onClose)
        PlaceEdit.Delete -> ConfirmDangerDialog(
            what = texts.places.deleteWhat.fill(place.name),
            explain = texts.places.deleteExplain,
            action = texts.places.delete,
            cancel = texts.close,
            onCancel = onClose
        ) {
            onDelete()
            onClose()
        }
    }
}

/** Новое название точки — окном с одним полем. */
@Composable
private fun RenameDialog(
    texts: CabinetTexts,
    place: RetailPlace,
    busy: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember(place.id) { mutableStateOf(place.name) }
    val changed = name.isNotBlank() && name.trim() != place.name
    FormDialog(
        title = texts.places.rename,
        icon = AppIcons.place,
        action = texts.places.rename,
        close = texts.close,
        busy = busy,
        missing = if (changed) emptyList() else listOf(texts.places.name),
        onDismiss = onDismiss,
        onAction = { onRename(name.trim()) }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(texts.places.name) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Переезд точки — окном: нынешний адрес для сверки, новый адрес из регистра
 * и место на карте. Окно закрывается, когда кабинет подтвердил переезд.
 */
@Composable
private fun MoveDialog(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: RetailPlace,
    busy: Boolean,
    onDismiss: () -> Unit
) {
    val model = placesViewModel(cabinet.cabinet)
    val draft = remember(place.id) { MoveDraft(place) }
    FormDialog(
        title = texts.places.changeAddress,
        icon = AppIcons.place,
        action = texts.places.changeAddress,
        close = texts.close,
        busy = busy,
        missing = if (draft.ready) emptyList() else listOf(texts.missing),
        onDismiss = onDismiss,
        onAction = {
            val address = draft.chosen
            val where = draft.point
            if (address != null && where != null) {
                model.move(place, address, where) {
                    draft.clear()
                    onDismiss()
                }
            }
        }
    ) { MoveFields(cabinet, texts, place, draft) }
}

/** Поля переезда: нынешний адрес, подбор нового и место на карте. */
@Composable
private fun MoveFields(cabinet: CabinetWindow, texts: CabinetTexts, place: RetailPlace, draft: MoveDraft) {
    val language = LocalLanguage.current
    val onAddress: (RegisterAddress) -> Unit = { draft.choose(it, language) }
    DetailLine(texts.places.address, addressIn(language, place.address, place.addressKz).ifBlank { Glyphs.DASH })
    AddressSearch(
        cabinet = cabinet,
        texts = texts,
        query = draft.query,
        onQuery = draft::type,
        owner = place.id,
        title = texts.places.changeAddress,
        onChoose = onAddress
    )
    if (draft.chosen != null) {
        Chip(texts.address.chosen, StatusColors.delivered)
    }
    // Пока новый адрес не выбран, карта открывается на нынешнем адресе точки:
    // переезжают обычно в соседний дом, а не в другой город.
    PlacePoint(cabinet, texts, draft.point, draft.here, onAddress) { draft.point = it }
}
