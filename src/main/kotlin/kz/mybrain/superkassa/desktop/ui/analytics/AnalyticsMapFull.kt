package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.desktop.ui.components.onEscape
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Карта касс во всё окно.
 *
 * В разделе карта делит высоту с отбором и карточкой, а ширину —
 * со списком касс, и на ней с трудом различались соседние дома.
 * Здесь она ложится поверх разделов кабинета, как окно просмотра
 * печатной формы: закрывается кнопкой в шапке, Escape или той же
 * кнопкой в углу карты, которой открылась.
 *
 * Слева — тот же список касс, что и в разделе, и та же карточка под ним:
 * выбор в списке ведёт карту к кассе, ярлычок на карте — открывает её
 * карточку. Список слева, а не справа: карта тянется в остаток ширины,
 * а глаз идёт от списка к карте, как в разделе — от отбора к карте.
 */
@Composable
internal fun AnalyticsMapFullscreen(parts: MapParts, onClose: () -> Unit) {
    // Фокус забирается сразу: Escape слышит только то, в чём стоит фокус,
    // а владелец жмёт его, не нажав прежде ни на что в окне.
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocus() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onEscape {
                    onClose()
                    true
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                FullscreenHead(parts.texts, parts.cabinetTexts, onClose)
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                    KkmColumn(parts)
                    MapWindow(parts, fullscreen = true, onFullscreen = onClose, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/** Шапка: чем занято окно и как из него выйти. */
@Composable
private fun FullscreenHead(texts: AnalyticsTexts, cabinet: CabinetTexts, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = texts.mapTab,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(AppIcons.close, contentDescription = cabinet.close)
        }
    }
}

/** Список касс и карточка выбранной — столбиком слева от карты. */
@Composable
private fun KkmColumn(parts: MapParts) {
    val model = parts.model
    Column(
        modifier = Modifier.width(Sizes.unplacedColumn).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        AnalyticsKkmList(
            placed = parts.placement.placed,
            unplaced = parts.placement.unplaced,
            chosen = model.chosen,
            source = model.source,
            texts = parts.texts,
            onChoose = { row -> model.show(row, parts.groups) },
            modifier = Modifier.weight(1f),
            sieved = model.sieve.set
        )
        UnderMap(model, parts.placement, parts.groups, parts.texts, parts.cabinetTexts, parts.panel)
    }
}
