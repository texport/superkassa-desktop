package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Обрамление окна карты: шапка, само полотно и подвал.
 *
 * Разметка обычная для окна приложения, и лежит она отдельно от самого
 * окна: в [MapPickerDialog] остаётся то, что окно делает, а не из каких
 * рядов оно составлено.
 */

/** Шапка окна: название слева, выход справа. */
@Composable
internal fun MapHeader(texts: CabinetTexts, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = texts.pickOnMap,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(AppIcons.close, contentDescription = texts.close)
        }
    }
}

/** Карта и управление ею. */
@Composable
internal fun MapArea(
    state: MapState,
    tiles: MapTiles,
    texts: CabinetTexts,
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Нажатие по карте ставит место точки: за этим окно и открыто.
        MapView(state, tiles, Modifier.fillMaxSize(), onTap = state::mark)
        MapControls(state, texts, preferences, Modifier.align(Alignment.TopEnd).padding(Spacing.snug))
    }
}

/** Что выбрано и что с этим делать. */
@Composable
internal fun MapFooter(
    state: MapState,
    texts: CabinetTexts,
    onDismiss: () -> Unit,
    onPicked: (BigDecimal, BigDecimal) -> Unit
) {
    val latitude = state.markerLatitude
    val longitude = state.markerLongitude
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (latitude == null || longitude == null) {
                    texts.pointNotChosen
                } else {
                    "${texts.latitude}: ${MapProjection.degrees(latitude)}${Glyphs.SEPARATOR}" +
                        "${texts.longitude}: ${MapProjection.degrees(longitude)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Под координатами сказано, что синий кружок — не выбранная
            // точка, а город: иначе владелец принял бы его за выбор.
            if (state.located) {
                Text(
                    text = if (state.locationPrecise) {
                        texts.map.myLocationPrecise
                    } else {
                        "${texts.map.myLocationShown}: ${state.locationCity}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onDismiss) { Text(texts.close) }
        Button(
            enabled = latitude != null && longitude != null,
            onClick = {
                if (latitude == null || longitude == null) return@Button
                onPicked(MapProjection.degrees(latitude), MapProjection.degrees(longitude))
                onDismiss()
            }
        ) { Text(texts.map.pickPoint) }
    }
}
