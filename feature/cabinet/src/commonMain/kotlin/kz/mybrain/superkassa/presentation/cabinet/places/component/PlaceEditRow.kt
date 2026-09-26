package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.mapPointOf
import kz.mybrain.superkassa.presentation.words.cabinet.addressIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Действия с торговой точкой: переименовать, сменить адрес, удалить.
 *
 * Точка живёт дольше кассы: магазин переименовывают и переезжают, а её
 * адрес уходит в регистрационное заявление и в чек.
 *
 * Три действия — три кнопки одного ряда, и каждое открывает своё окно:
 * прежде поле названия, подбор адреса, место на карте и три кнопки шли
 * в карточке подряд, и было не видно, что к чему относится. Удаление —
 * необратимое — спрашивает подтверждения и отмечено цветом ошибки; пока
 * к точке привязаны кассы, кнопка погашена, и рядом сказано почему.
 *
 * Переезд — два шага в одном окне: адрес из регистра и место на карте,
 * затем подтверждение. Прежде выбор адреса переселял точку немедленно,
 * подставив в координаты нули, если поля были пусты.
 */
@Composable
internal fun PlaceEditRow(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: RetailPlace,
    busy: Boolean,
    onDelete: () -> Unit
) {
    var open by remember(place.id) { mutableStateOf<PlaceEdit?>(null) }
    val close = { open = null }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(enabled = !busy, onClick = { open = PlaceEdit.Rename }) { Text(texts.places.rename) }
        FilledTonalButton(enabled = !busy, onClick = { open = PlaceEdit.Move }) { Text(texts.places.changeAddress) }
        PlaceDeletion(texts, place, busy) { open = PlaceEdit.Delete }
    }
    open?.let { PlaceEditDialog(it, cabinet, texts, place, busy, onDelete, close) }
}

/** Какое окно правки точки открыто. */
internal enum class PlaceEdit { Rename, Move, Delete }

/**
 * Удаление — кнопкой цвета ошибки, и объяснение, когда его нет.
 *
 * Кнопка остаётся на месте и погашенной: убранная целиком, она оставляла
 * владельца без понимания, что точку вообще можно удалить. Рядом с погашенной
 * стоит причина — привязанная касса.
 */
@Composable
private fun PlaceDeletion(texts: CabinetTexts, place: RetailPlace, busy: Boolean, onDelete: () -> Unit) {
    val held = place.cashRegisterCount > 0
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.inline), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            enabled = !held && !busy,
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text(texts.places.delete) }
        if (held) InfoTip(texts.places.removeBlocked)
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
