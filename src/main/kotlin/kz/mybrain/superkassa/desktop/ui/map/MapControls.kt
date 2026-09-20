package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Управление картой: своё место, приближение и разрешение на определение.
 *
 * Отделено от окна выбора: то отвечает за поиск адреса и за то, что
 * уйдёт в кабинет, а здесь — работа с самой картой и разговор
 * о разрешении.
 */

/**
 * Управление картой: своё место, приближение, отдаление.
 *
 * Кнопки лежат на своей поверхности, а не прямо на плитках: значок
 * без подложки терялся на пёстрой карте — на светлом квартале его
 * не было видно вовсе.
 */
@Composable
internal fun MapControls(
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
                Icon(AppIcons.zoomIn, contentDescription = texts.map.zoomIn)
            }
            IconButton(onClick = { state.zoomBy(-1) }) {
                Icon(AppIcons.zoomOut, contentDescription = texts.map.zoomOut)
            }
        }
    }
}

/**
 * «Где я»: сперва служба геопозиции самой машины, потом — адрес подключения.
 *
 * У макбука служба своя, и точность у неё домовая: разрешение на неё
 * спрашивает система своим окном, приложение к нему не прикасается.
 * Свой вопрос остаётся только для запасного пути — там наружу уходит
 * адрес подключения, и это решение владельца.
 *
 * Метку выбранной точки кнопка не ставит ни в том, ни в другом случае:
 * своё место — это своё место, а точку выбирает владелец нажатием.
 */
@Composable
private fun LocateButton(state: MapState, texts: CabinetTexts, preferences: Preferences) {
    val scope = rememberCoroutineScope()
    val locator = remember { MapServices(preferences).locator }
    var asking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    /** Запасной путь: город по адресу подключения — и только по разрешению. */
    suspend fun byConnection() {
        locator.locate()?.let { state.showLocation(it.latitude, it.longitude, it.city, CITY_ZOOM) }
    }

    fun locate() = scope.launch {
        busy = true
        // Сначала спрашиваем саму машину: её служба геопозиции указывает
        // на дом, и разрешение у владельца просит система своим окном.
        val system = MacLocation.locate()
        if (system != null) {
            state.showLocation(system.latitude, system.longitude, "", HOUSE_ZOOM, precise = true)
        } else {
            when (preferences.locationAllowed) {
                true -> byConnection()
                false -> Unit
                null -> asking = true
            }
        }
        busy = false
    }

    IconButton(enabled = !busy, onClick = { locate() }) {
        Icon(AppIcons.myLocation, contentDescription = texts.map.myLocation)
    }
    if (asking) {
        LocationConsent(
            texts = texts,
            onAllow = {
                preferences.locationAllowed = true
                asking = false
                scope.launch { byConnection() }
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
        title = { Text(texts.map.locationAsk) },
        text = { Text(texts.map.locationAskHint) },
        confirmButton = { Button(onClick = onAllow) { Text(texts.map.locationAllow) } },
        dismissButton = { TextButton(onClick = onDeny) { Text(texts.map.locationDeny) } }
    )
}
