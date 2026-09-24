package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings

/**
 * Чьей картой пользуется рабочее место.
 *
 * Плитки, поиск адреса, место по метке и определение своего места
 * берутся у сторонних служб.
 * По умолчанию это открытые службы сообщества: они годятся для стенда,
 * но их правила запрещают массовую выкачку, и выпускать на них всех
 * владельцев нельзя. Здесь каждая заменяется своей или оплаченной —
 * без перевыпуска приложения.
 *
 * Пустое поле означает общедоступную службу, и её адрес виден в поле
 * подсказкой: отдельной строки «сейчас используется такая-то» не нужно.
 */
@Composable
fun MapServicesCard(workplace: WorkplaceSettingsUiState, actions: WorkplaceSettingsActions) {
    val texts = LocalStrings.current.settings
    val maps = workplace.mapFields
    val standard = workplace.publicMaps
    SectionCard(title = texts.mapServices, info = texts.mapServicesHint) {
        ServiceField(texts.mapTiles, maps.tiles, standard.tiles) { actions.typeMaps(maps.copy(tiles = it)) }
        ServiceField(texts.mapSearch, maps.search, standard.search) { actions.typeMaps(maps.copy(search = it)) }
        ServiceField(texts.mapReverse, maps.reverse, standard.reverse) { actions.typeMaps(maps.copy(reverse = it)) }
        ServiceField(texts.mapLocation, maps.location, standard.location) {
            actions.typeMaps(maps.copy(location = it))
        }
        WrapRow {
            FilledTonalButton(enabled = workplace.mapsChanged, onClick = actions::saveMaps) { Text(texts.save) }
            TextButton(enabled = workplace.mapsCustom, onClick = actions::resetMaps) { Text(texts.mapDefault) }
        }
    }
}

/** Поле адреса службы: пустое означает общедоступную, и она видна подсказкой. */
@Composable
private fun ServiceField(label: String, value: String?, community: String?, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value.orEmpty(),
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(community.orEmpty(), style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
