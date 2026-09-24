package kz.mybrain.superkassa.presentation.analytics.map

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.designsystem.adaptive.windowMargin
import kz.mybrain.superkassa.designsystem.keyboard.CloseOnEscape
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.analytics.map.component.UnderMap
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
    CloseOnEscape(onClose)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(windowMargin),
                verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
            ) {
                FullscreenHead(parts.texts, parts.cabinetTexts, onClose)
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    // Список справа от карты, как и в разделе: карта читается
                    // первой, и место списка не меняется при раскрытии.
                    MapWindow(parts, fullscreen = true, onFullscreen = onClose, Modifier.weight(1f).fillMaxHeight())
                    KkmColumn(parts)
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
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

/**
 * Список касс и карточка выбранной — столбиком слева от карты.
 *
 * Карточка под списком берёт свою высоту, но не всю: в низком окне
 * «Аналитика кассы» уходила за нижний край.
 */
@Composable
private fun KkmColumn(parts: MapParts) {
    ListOverCard(
        modifier = Modifier.width(Sizes.unplacedColumn).fillMaxHeight(),
        list = { KkmList(parts) },
        card = { UnderMap(parts) }
    )
}
