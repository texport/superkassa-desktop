package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.AddressLevel
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.address.AddressSearch
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.map.MapPickerDialog
import kz.mybrain.superkassa.presentation.map.MapPoint
import kz.mybrain.superkassa.presentation.map.MapRegistry
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Место торговой точки — строкой и кнопкой карты.
 *
 * Полей широты и долготы в форме больше нет. В адресном регистре
 * координат нет, взять их владельцу неоткуда, и два поля с подписью
 * «Координаты точки в градусах: их вводит владелец» занимали четыре
 * строки, ничего не объясняя. Место указывается на карте, а набрать
 * градусы руками можно там же — в окне карты, где это нужно тем, у кого
 * координаты уже есть.
 *
 * @param address выбранный в регистре адрес: карта откроется на нём,
 *   и в самой карте адрес выбирается тем же регистром.
 * @param onAddress адрес, выбранный в окне карты: он и адрес формы —
 *   одна и та же запись регистра.
 */
@Composable
fun PlacePoint(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    point: MapPoint?,
    address: RegisterAddress?,
    onAddress: (RegisterAddress) -> Unit,
    onPoint: (MapPoint) -> Unit
) {
    var onMap by remember { mutableStateOf(false) }
    // Ряд переносится, а не сжимает строку: в узкой карточке кнопка
    // оставляла координатам столбик в букву шириной, и «Широта» читалась
    // сверху вниз. Строка уступает кнопке место, пока не станет уже
    // прежнего поля формы, и тогда кнопка уходит под неё.
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.fieldGap) {
        Text(
            text = point?.let { "${texts.latitude}: ${it.latitude}${Glyphs.SEPARATOR}${texts.longitude}: ${it.longitude}" }
                ?: texts.pointNotChosen,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).widthIn(min = Sizes.fieldForm)
        )
        FieldButton(text = texts.pickOnMap) { onMap = true }
    }
    if (onMap) {
        MapPickerDialog(
            services = cabinet.app.areas.analytics.map,
            registry = remember(cabinet) { CabinetRegistry(cabinet) },
            point = point,
            address = address,
            onAddress = onAddress,
            onDismiss = { onMap = false },
            onPicked = onPoint
        )
    }
}

/**
 * Адресный регистр кабинета для окна карты.
 *
 * Обращения идут работой кабинета окна: его занятость и его слова
 * о помехах — те же, что у формы точки под окном.
 */
internal class CabinetRegistry(private val window: CabinetWindow) : MapRegistry {
    private val cabinet = window.cabinet

    override val open: Boolean get() = cabinet.state.value.open

    override suspend fun lookUp(level: AddressLevel, parentId: Long, query: String): List<AddressSuggestion> =
        cabinet.work.run("address lookup") { cabinet.useCases.lookUpAddress(level, parentId, query) }.value.orEmpty()

    override suspend fun resolve(rka: String): RegisterAddress? =
        cabinet.work.run("address resolve") { cabinet.useCases.resolveAddress(rka) }.value

    @Composable
    override fun Search(query: String, onQuery: (String) -> Unit, owner: Any?, onChoose: (RegisterAddress) -> Unit) {
        val texts = cabinetTexts(LocalLanguage.current)
        AddressSearch(window, texts, query, onQuery, owner, onChoose = onChoose)
    }
}
