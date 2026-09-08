package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Выбор места торговой точки на карте.
 *
 * Открывается окном из формы точки: карта нужна на минуту, а форма
 * с картой внутри перестала бы читаться. Выбранное возвращается
 * градусами — теми же, что владелец мог бы набрать руками; ручной ввод
 * остаётся на месте, потому что карта требует сети, а касса стоит и там,
 * где сети нет.
 *
 * @param latitude уже известная широта: карта откроется на ней.
 */
@Composable
fun MapPickerDialog(
    texts: CabinetTexts,
    latitude: BigDecimal?,
    longitude: BigDecimal?,
    onDismiss: () -> Unit,
    onPicked: (BigDecimal, BigDecimal) -> Unit
) {
    val tiles = remember { MapTiles() }
    val state = remember {
        MapState().also {
            if (latitude != null && longitude != null) it.show(latitude.toDouble(), longitude.toDouble())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(Sizes.mapWidth),
        icon = { Icon(AppIcons.place, contentDescription = null) },
        title = { Text(texts.pickOnMap) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
                Text(
                    text = texts.pickOnMapHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.fillMaxWidth().height(Sizes.mapHeight)) {
                    MapView(state, tiles, Modifier.fillMaxWidth().height(Sizes.mapHeight)) { _, _ -> }
                    ZoomButtons(state, texts, Modifier.align(Alignment.TopEnd))
                }
                PickedPoint(state, texts)
            }
        },
        confirmButton = {
            Button(
                enabled = state.marked,
                onClick = {
                    val chosenLatitude = state.markerLatitude ?: return@Button
                    val chosenLongitude = state.markerLongitude ?: return@Button
                    onPicked(MapProjection.degrees(chosenLatitude), MapProjection.degrees(chosenLongitude))
                    onDismiss()
                }
            ) { Text(texts.pickPoint) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(texts.close) } }
    )
}

/** Приближение и отдаление: колесо мыши на карте есть не у каждого рабочего места. */
@Composable
private fun ZoomButtons(state: MapState, texts: CabinetTexts, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Spacing.tight), verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        IconButton(onClick = { state.zoomBy(1) }) {
            Icon(AppIcons.zoomIn, contentDescription = texts.zoomIn)
        }
        IconButton(onClick = { state.zoomBy(-1) }) {
            Icon(AppIcons.zoomOut, contentDescription = texts.zoomOut)
        }
    }
}

/** Что выбрано — теми же градусами, что уйдут в кабинет. */
@Composable
private fun PickedPoint(state: MapState, texts: CabinetTexts) {
    val latitude = state.markerLatitude
    val longitude = state.markerLongitude
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Text(
            text = if (latitude == null || longitude == null) {
                texts.pointNotChosen
            } else {
                "${texts.latitude}: ${MapProjection.degrees(latitude)} · " +
                    "${texts.longitude}: ${MapProjection.degrees(longitude)}"
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
