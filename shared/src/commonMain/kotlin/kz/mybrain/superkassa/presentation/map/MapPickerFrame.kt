package kz.mybrain.superkassa.presentation.map

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
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.mapview.MapControls
import kz.mybrain.superkassa.presentation.common.mapview.MapLocating
import kz.mybrain.superkassa.presentation.common.mapview.MapState
import kz.mybrain.superkassa.presentation.common.mapview.MapTiles
import kz.mybrain.superkassa.presentation.common.mapview.MapView
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
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
    locating: MapLocating,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Нажатие по карте ставит место точки: за этим окно и открыто.
        MapView(state, tiles, texts.map, Modifier.fillMaxSize(), onTap = state::mark)
        MapControls(state, texts, locating, Modifier.align(Alignment.TopEnd).padding(Spacing.fieldGap))
    }
}

/** Что выбрано и что с этим делать. */
@Composable
internal fun MapFooter(state: MapState, texts: CabinetTexts, onDismiss: () -> Unit, onPicked: (MapPoint) -> Unit) {
    val latitude = state.markerLatitude
    val longitude = state.markerLongitude
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarkerWords(state, texts, Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text(texts.close) }
        Button(
            enabled = latitude != null && longitude != null,
            onClick = {
                if (latitude == null || longitude == null) return@Button
                onPicked(MapPoint(cabinetDegrees(latitude), cabinetDegrees(longitude)))
                onDismiss()
            }
        ) { Text(texts.map.pickPoint) }
    }
}

/** Координаты метки и, если место определено, что значит кружок в ореоле. */
@Composable
private fun MarkerWords(state: MapState, texts: CabinetTexts, modifier: Modifier) {
    val latitude = state.markerLatitude
    val longitude = state.markerLongitude
    Column(modifier = modifier) {
        Text(
            text = if (latitude == null || longitude == null) {
                texts.pointNotChosen
            } else {
                "${texts.latitude}: ${cabinetDegrees(latitude)}${Glyphs.SEPARATOR}" +
                    "${texts.longitude}: ${cabinetDegrees(longitude)}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.located) LocationWords(state, texts)
    }
}

/**
 * Под координатами сказано, что кружок в ореоле — не выбранная точка,
 * а город: иначе владелец принял бы его за выбор. Цвет в надписи не назван:
 * выбранная точка красится главной ролью схемы, а своё место — третичной,
 * и «синий» указывал на первую.
 */
@Composable
private fun LocationWords(state: MapState, texts: CabinetTexts) {
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
