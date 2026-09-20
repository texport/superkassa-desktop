package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.map.MapService
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
fun MapServicesCard(session: Session) {
    val texts = LocalStrings.current.settings
    val preferences = session.preferences
    var tiles by remember { mutableStateOf(preferences.maps.tiles.orEmpty()) }
    var search by remember { mutableStateOf(preferences.maps.search.orEmpty()) }
    var reverse by remember { mutableStateOf(preferences.maps.reverse.orEmpty()) }
    var location by remember { mutableStateOf(preferences.maps.location.orEmpty()) }

    fun save() {
        preferences.maps.tiles = tiles.trim().ifBlank { null }
        preferences.maps.search = search.trim().ifBlank { null }
        preferences.maps.reverse = reverse.trim().ifBlank { null }
        preferences.maps.location = location.trim().ifBlank { null }
    }

    fun toDefaults() {
        tiles = ""
        search = ""
        reverse = ""
        location = ""
        save()
    }

    SectionCard(title = texts.mapServices, info = texts.mapServicesHint) {
        ServiceField(texts.mapTiles, tiles, MapService.TILES) { tiles = it }
        ServiceField(texts.mapSearch, search, MapService.SEARCH) { search = it }
        ServiceField(texts.mapReverse, reverse, MapService.REVERSE) { reverse = it }
        ServiceField(texts.mapLocation, location, MapService.LOCATION) { location = it }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
            FilledTonalButton(
                enabled = changed(preferences, ServiceUrls(tiles, search, reverse, location)),
                onClick = ::save
            ) { Text(texts.save) }
            TextButton(
                enabled = listOf(tiles, search, reverse, location).any { it.isNotBlank() },
                onClick = ::toDefaults
            ) { Text(texts.mapDefault) }
        }
    }
}

/** Набранные в карточке адреса служб — одним значением: их четыре, и ходят они вместе. */
private data class ServiceUrls(val tiles: String, val search: String, val reverse: String, val location: String)

/** Отличается ли набранное от записанного: сохранять то же самое незачем. */
private fun changed(preferences: Preferences, urls: ServiceUrls): Boolean =
    urls.tiles.trim() != preferences.maps.tiles.orEmpty() ||
        urls.search.trim() != preferences.maps.search.orEmpty() ||
        urls.reverse.trim() != preferences.maps.reverse.orEmpty() ||
        urls.location.trim() != preferences.maps.location.orEmpty()

/** Поле адреса службы: пустое означает общедоступную, и она видна подсказкой. */
@Composable
private fun ServiceField(label: String, value: String, community: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(community, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
