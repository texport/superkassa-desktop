package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Выбор места торговой точки на карте.
 *
 * Собрано на `Dialog` с собственной поверхностью, а не на `AlertDialog`:
 * у того содержимое живёт в прокручиваемой средней части с рассчитанной
 * высотой, и карта в четыреста точек её распирала — заголовок с подсказкой
 * уходили за верхний край окна, а строка координат наезжала на карту.
 *
 * Разметка обычная для окна приложения: шапка с названием и выходом,
 * подсказка, карта, под ней выбранное и действия.
 */
@Composable
fun MapPickerDialog(
    texts: CabinetTexts,
    preferences: Preferences,
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            // Высота окна задана, а карта берёт остаток: при высоте
            // по содержимому карта распирала окно, и подсказку с шапкой
            // выдавливало за верхний край.
            modifier = Modifier.width(Sizes.mapWidth).height(Sizes.mapDialogHeight),
            shape = RoundedCornerShape(Sizes.corner),
            tonalElevation = Sizes.dialogElevation
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.normal),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                MapHeader(texts, onDismiss)
                Text(
                    text = texts.pickOnMapHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MapArea(state, tiles, texts, preferences, Modifier.weight(1f))
                MapFooter(state, texts, onDismiss, onPicked)
            }
        }
    }
}

/** Шапка окна: название слева, выход справа. */
@Composable
private fun MapHeader(texts: CabinetTexts, onDismiss: () -> Unit) {
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
private fun MapArea(
    state: MapState,
    tiles: MapTiles,
    texts: CabinetTexts,
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        MapView(state, tiles, Modifier.fillMaxSize()) { _, _ -> }
        MapControls(state, texts, preferences, Modifier.align(Alignment.TopEnd).padding(Spacing.snug))
    }
}

/**
 * Управление картой: своё место, приближение, отдаление.
 *
 * Кнопки лежат на своей поверхности, а не прямо на плитках: значок
 * без подложки терялся на пёстрой карте — на светлом квартале его
 * не было видно вовсе.
 */
@Composable
private fun MapControls(
    state: MapState,
    texts: CabinetTexts,
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Sizes.corner),
        tonalElevation = Sizes.dialogElevation
    ) {
        Column {
            LocateButton(state, texts, preferences)
            IconButton(onClick = { state.zoomBy(1) }) {
                Icon(AppIcons.zoomIn, contentDescription = texts.zoomIn)
            }
            IconButton(onClick = { state.zoomBy(-1) }) {
                Icon(AppIcons.zoomOut, contentDescription = texts.zoomOut)
            }
        }
    }
}

/**
 * «Где я»: спрашивает разрешение и ведёт карту в найденный город.
 *
 * Разрешение спрашивается один раз и запоминается — так же, как это
 * делает браузер. Метку кнопка не ставит: место определяется до города,
 * и поставленная метка выглядела бы выбранной точкой.
 */
@Composable
private fun LocateButton(state: MapState, texts: CabinetTexts, preferences: Preferences) {
    val scope = rememberCoroutineScope()
    val locator = remember { MapLocator() }
    var asking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun locate() = scope.launch {
        busy = true
        locator.locate()?.let { state.showLocation(it.latitude, it.longitude, it.city, CITY_ZOOM) }
        busy = false
    }

    IconButton(
        enabled = !busy && preferences.locationAllowed != false,
        onClick = { if (preferences.locationAllowed == true) locate() else asking = true }
    ) {
        Icon(AppIcons.myLocation, contentDescription = texts.myLocation)
    }
    if (asking) {
        LocationConsent(
            texts = texts,
            onAllow = {
                preferences.locationAllowed = true
                asking = false
                locate()
            },
            onDeny = {
                preferences.locationAllowed = false
                asking = false
            }
        )
    }
}

/**
 * Вопрос об определении места.
 *
 * Сказано ровно то, что произойдёт: наружу уйдёт адрес подключения,
 * а обратно придёт город — не дом. Умолчание здесь было бы обманом:
 * владелец вправе знать, что кассa обратилась в чужую службу.
 */
@Composable
private fun LocationConsent(texts: CabinetTexts, onAllow: () -> Unit, onDeny: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDeny,
        icon = { Icon(AppIcons.myLocation, contentDescription = null) },
        title = { Text(texts.locationAsk) },
        text = { Text(texts.locationAskHint) },
        confirmButton = { Button(onClick = onAllow) { Text(texts.locationAllow) } },
        dismissButton = { TextButton(onClick = onDeny) { Text(texts.locationDeny) } }
    )
}

/** Что выбрано и что с этим делать. */
@Composable
private fun MapFooter(
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
                    "${texts.latitude}: ${MapProjection.degrees(latitude)} · " +
                        "${texts.longitude}: ${MapProjection.degrees(longitude)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Под координатами сказано, что синий кружок — не выбранная
            // точка, а город: иначе владелец принял бы его за выбор.
            if (state.located) {
                Text(
                    text = "${texts.myLocationShown}: ${state.locationCity}",
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
        ) { Text(texts.pickPoint) }
    }
}

/** Увеличение, на котором виден город: с него начинается и найденное место. */
private const val CITY_ZOOM = 12
