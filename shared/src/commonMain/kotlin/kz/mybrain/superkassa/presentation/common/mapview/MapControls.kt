package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes

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
 *
 * @param more кнопки, которые нужны только одной из карт: карта касс
 *   раскрывается во всё окно, карте выбора места это ни к чему. Стоят
 *   на той же подложке, а не рядом: два столбика кнопок в углу читались
 *   бы как два разных управления.
 */
@Composable
internal fun MapControls(
    state: MapState,
    texts: CabinetTexts,
    locating: MapLocating,
    modifier: Modifier = Modifier,
    more: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Sizes.corner),
        tonalElevation = Sizes.dialogElevation
    ) {
        Column {
            LocateButton(state, texts, locating)
            // У предела кнопка гаснет: нажатие, которое ничего не меняет, читалось как поломка.
            IconButton(onClick = { state.zoomBy(1) }, enabled = state.zoom < MAX_ZOOM) {
                Icon(AppIcons.zoomIn, contentDescription = texts.map.zoomIn)
            }
            IconButton(onClick = { state.zoomBy(-1) }, enabled = state.zoom > MIN_ZOOM) {
                Icon(AppIcons.zoomOut, contentDescription = texts.map.zoomOut)
            }
            more()
        }
    }
}

/** «Где я»: как искать своё место, решает [MapLocating]; здесь кнопка и вопрос владельцу. */
@Composable
private fun LocateButton(state: MapState, texts: CabinetTexts, locating: MapLocating) {
    val scope = rememberCoroutineScope()
    IconButton(enabled = !locating.busy, onClick = { scope.launch { locating.locate(state) } }) {
        Icon(AppIcons.myLocation, contentDescription = texts.map.myLocation)
    }
    if (locating.asking) {
        LocationConsent(
            texts = texts,
            onAllow = { scope.launch { locating.allow(state) } },
            onDeny = { scope.launch { locating.deny() } }
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
    AlertDialog(
        onDismissRequest = onDeny,
        icon = { Icon(AppIcons.myLocation, contentDescription = null) },
        title = { Text(texts.map.locationAsk) },
        text = { Text(texts.map.locationAskHint) },
        confirmButton = { Button(onClick = onAllow) { Text(texts.map.locationAllow) } },
        dismissButton = { TextButton(onClick = onDeny) { Text(texts.map.locationDeny) } }
    )
}
