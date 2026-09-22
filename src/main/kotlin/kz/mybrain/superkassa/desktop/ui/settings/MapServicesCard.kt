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
    // Набранное переживает уход в другой раздел: экран настроек уходит
    // из состава вместе с ним, и поля забывали набранное молча.
    val tiles = SettingsDrafts.of(SettingsDrafts.Field.MAP_TILES, preferences.maps.tiles.orEmpty())
    val search = SettingsDrafts.of(SettingsDrafts.Field.MAP_SEARCH, preferences.maps.search.orEmpty())
    val reverse = SettingsDrafts.of(SettingsDrafts.Field.MAP_REVERSE, preferences.maps.reverse.orEmpty())
    val location = SettingsDrafts.of(SettingsDrafts.Field.MAP_LOCATION, preferences.maps.location.orEmpty())

    fun save() {
        preferences.maps.tiles = tiles.trim().ifBlank { null }
        preferences.maps.search = search.trim().ifBlank { null }
        preferences.maps.reverse = reverse.trim().ifBlank { null }
        preferences.maps.location = location.trim().ifBlank { null }
        MAP_FIELDS.forEach(SettingsDrafts::forget)
    }

    fun toDefaults() {
        MAP_FIELDS.forEach { SettingsDrafts.type(it, "") }
        save()
    }

    SectionCard(title = texts.mapServices, info = texts.mapServicesHint) {
        ServiceField(texts.mapTiles, tiles, MapService.TILES) {
            SettingsDrafts.type(SettingsDrafts.Field.MAP_TILES, it)
        }
        ServiceField(texts.mapSearch, search, MapService.SEARCH) {
            SettingsDrafts.type(SettingsDrafts.Field.MAP_SEARCH, it)
        }
        ServiceField(texts.mapReverse, reverse, MapService.REVERSE) {
            SettingsDrafts.type(SettingsDrafts.Field.MAP_REVERSE, it)
        }
        ServiceField(texts.mapLocation, location, MapService.LOCATION) {
            SettingsDrafts.type(SettingsDrafts.Field.MAP_LOCATION, it)
        }
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

/** Поля адресов карты: их четыре, и сохраняются они вместе. */
private val MAP_FIELDS = listOf(
    SettingsDrafts.Field.MAP_TILES,
    SettingsDrafts.Field.MAP_SEARCH,
    SettingsDrafts.Field.MAP_REVERSE,
    SettingsDrafts.Field.MAP_LOCATION
)

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
