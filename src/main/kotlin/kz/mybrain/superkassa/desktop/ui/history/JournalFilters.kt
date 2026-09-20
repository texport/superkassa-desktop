package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.MenuChip
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Отбор журнала: вид документа, состояние доставки и смена.
 *
 * Показывается только то, что в пришедших строках действительно есть:
 * отбор по виду, которого за срок не было, или по состоянию, в котором
 * ни один документ не оказался, обещает разделение, за которым пустой
 * список. Поэтому наборы приходят снаружи — из того, что прочитано.
 *
 * Виды и состояния — плашками: их единицы, и нажатие на них сразу видно.
 * Смена — тоже плашкой, но с выпадающим списком: за месяц смен шестьдесят,
 * и шестьдесят плашек заняли бы пол-экрана, а поле ввода в ряду плашек
 * торчало вдвое выше соседей.
 */
@Composable
fun JournalFilters(
    journal: HistoryJournalTexts,
    types: List<JournalType>,
    deliveries: List<JournalDelivery>,
    shifts: List<Long>,
    query: JournalQuery,
    onQuery: (JournalQuery) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        TypeChips(journal, types, query, onQuery)
        DeliveryChips(journal, deliveries, query, onQuery)
        ShiftChip(journal, shifts, query, onQuery)
    }
}

/** Отбор по виду документа: названия — от источника, а не свои. */
@Composable
private fun TypeChips(
    journal: HistoryJournalTexts,
    types: List<JournalType>,
    query: JournalQuery,
    onQuery: (JournalQuery) -> Unit
) {
    if (types.isEmpty()) return
    ChipGroup(journal.documentType) {
        FilterChip(
            selected = query.type == null,
            onClick = { onQuery(query.copy(type = null)) },
            label = { Text(journal.allTypes) }
        )
        types.forEach { type ->
            FilterChip(
                selected = query.type == type.code,
                onClick = { onQuery(query.copy(type = type.code)) },
                label = { Text(type.title) }
            )
        }
    }
}

/** Отбор по состоянию доставки: доставлен, в очереди, отклонён. */
@Composable
private fun DeliveryChips(
    journal: HistoryJournalTexts,
    deliveries: List<JournalDelivery>,
    query: JournalQuery,
    onQuery: (JournalQuery) -> Unit
) {
    if (deliveries.size < 2) return
    val states = LocalStrings.current.status
    ChipGroup(journal.deliveryState) {
        FilterChip(
            selected = query.delivery == null,
            onClick = { onQuery(query.copy(delivery = null)) },
            label = { Text(journal.allStates) }
        )
        deliveries.forEach { state ->
            FilterChip(
                selected = query.delivery == state,
                onClick = { onQuery(query.copy(delivery = state)) },
                label = { Text(state.title(states)) }
            )
        }
    }
}

/**
 * Отбор по смене: за месяц их десятки, поэтому выпадающим списком.
 *
 * Подпись и рост — от плашки, как у отбора по виду и по состоянию:
 * три отбора стоят одним рядом, и разного размера им быть не за что.
 */
@Composable
private fun ShiftChip(
    journal: HistoryJournalTexts,
    shifts: List<Long>,
    query: JournalQuery,
    onQuery: (JournalQuery) -> Unit
) {
    if (shifts.isEmpty()) return
    ChipGroup(journal.colShift) {
        MenuChip(
            value = shiftLabel(query.shiftNo, journal),
            options = listOf(null) + shifts,
            title = { shiftLabel(it, journal) },
            chosen = query.shiftNo != null,
            onSelect = { onQuery(query.copy(shiftNo = it)) }
        )
    }
}

/** Как названа смена в отборе: номер, а «ничего не выбрано» — словами. */
internal fun shiftLabel(shift: Long?, journal: HistoryJournalTexts): String =
    shift?.toString() ?: journal.allShifts

/** Подпись отбора и его плашки одной строкой. */
@Composable
private fun ChipGroup(title: String, chips: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        chips()
    }
}
