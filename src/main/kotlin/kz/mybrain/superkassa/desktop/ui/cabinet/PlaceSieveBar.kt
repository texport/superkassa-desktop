package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.MenuChip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Ряд отбора и порядка над списком точек.
 *
 * Собран теми же плашками, что отбор над картой аналитики, — владелец
 * работает в обоих разделах и не должен привыкать к двум разным способам
 * спросить об одном. Но колонка узкая, четыреста точек шириной, и набора
 * плашек на каждый смысл учёта в неё не встаёт: учёт и порядок выбираются
 * плашкой с выпадающим списком, как смена в журнале документов.
 *
 * Блокировка — нажимаемая плашка: она складывается с учётом, а не
 * исключает его. Пока кабинет о блокировках не ответил, плашка погашена:
 * нажатая на непрочитанном, она показала бы пустой список там, где
 * заблокированных, может, и нет.
 *
 * @param locksKnown ответил ли кабинет, какие кассы заблокированы.
 */
@Composable
internal fun PlaceSieveBar(
    texts: CabinetTexts,
    sieve: PlaceSieve,
    locksKnown: Boolean,
    onSieve: (PlaceSieve) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(end = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
        // Плашки и значок стороны читаются одним рядом, а не двумя
        // уровнями: по верхнему краю значок висел бы над плашками.
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        RecordChip(texts, sieve, onSieve)
        BlockedChip(texts, sieve, locksKnown, onSieve)
        OrderChip(texts, sieve, onSieve)
        DirectionButton(texts, sieve, onSieve)
        if (sieve.set) {
            // Порядок сбросом не трогается: владелец выстроил список под
            // себя, и сброшенный отбор не повод перетряхивать его снова.
            TextButton(onClick = { onSieve(PlaceSieve(order = sieve.order, descending = sieve.descending)) }) {
                Text(texts.sieve.clear)
            }
        }
    }
}

/** Учёт КГД: пять смыслов, и выбирается один — они исключают друг друга. */
@Composable
private fun RecordChip(texts: CabinetTexts, sieve: PlaceSieve, onSieve: (PlaceSieve) -> Unit) {
    MenuChip(
        value = recordTitle(sieve.record, texts.sieve),
        options = listOf(null) + KkmRecord.entries,
        title = { recordTitle(it, texts.sieve) },
        chosen = sieve.record != null,
        onSelect = { onSieve(sieve.copy(record = it)) }
    )
}

/** Заблокированные: признак складывается с учётом, поэтому плашка нажимаемая. */
@Composable
private fun BlockedChip(
    texts: CabinetTexts,
    sieve: PlaceSieve,
    locksKnown: Boolean,
    onSieve: (PlaceSieve) -> Unit
) {
    FilterChip(
        selected = sieve.blocked,
        enabled = locksKnown,
        onClick = { onSieve(sieve.copy(blocked = !sieve.blocked)) },
        label = { Text(texts.sieve.blocked) },
        leadingIcon = { if (sieve.blocked) Icon(AppIcons.chosen, contentDescription = null) }
    )
}

/** По чему выстроен список: признаков четыре, и выбирается один. */
@Composable
private fun OrderChip(texts: CabinetTexts, sieve: PlaceSieve, onSieve: (PlaceSieve) -> Unit) {
    MenuChip(
        value = sieve.order.title(texts),
        options = PlaceOrder.entries,
        title = { it.title(texts) },
        chosen = sieve.order != PlaceOrder.Name,
        onSelect = { onSieve(sieve.copy(order = it)) }
    )
}

/**
 * Сторона порядка — значком рядом, как в журнале документов.
 *
 * Значок ростом с плашку, а не обычной кнопкой в сорок восемь точек:
 * в ряду плашек кнопка торчала бы выше соседей, и ряд читался бы
 * как два разных.
 */
@Composable
private fun DirectionButton(texts: CabinetTexts, sieve: PlaceSieve, onSieve: (PlaceSieve) -> Unit) {
    IconButton(
        onClick = { onSieve(sieve.copy(descending = !sieve.descending)) },
        modifier = Modifier.size(Sizes.chipHeight)
    ) {
        Icon(
            imageVector = if (sieve.descending) AppIcons.descending else AppIcons.ascending,
            contentDescription = if (sieve.descending) texts.descending else texts.ascending,
            modifier = Modifier.size(Sizes.chipIcon)
        )
    }
}
