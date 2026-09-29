package kz.mybrain.superkassa.presentation.common.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.picker.MenuChip
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.journal.HistoryJournalTexts

/**
 * Отбор журнала: вид документа, состояние доставки и смена.
 *
 * Показывается только то, что в пришедших строках действительно есть:
 * отбор по виду, которого за срок не было, или по состоянию, в котором
 * ни один документ не оказался, обещает разделение, за которым пустой
 * список. Поэтому наборы приходят снаружи — из того, что прочитано.
 *
 * Каждый отбор — плашкой с выпадающим списком (Material 3, Chips → Filter
 * chips with menu), и выбранное стоит на самой плашке. Плашки на каждый
 * вид и состояние переносились в два-три ряда, не помещались в отведённую
 * отбору долю высоты, и нижний ряд срезался посередине пустыми рамками;
 * строка, листаемая вбок, не листалась колесом мыши.
 */
@Composable
internal fun JournalFilters(
    journal: HistoryJournalTexts,
    types: List<JournalType>,
    deliveries: List<JournalDelivery>,
    shifts: List<Long>,
    query: JournalQuery,
    onQuery: (JournalQuery) -> Unit
) {
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.fieldGap) {
        TypeChip(journal, types, query, onQuery)
        DeliveryChip(journal, deliveries, query, onQuery)
        ShiftChip(journal, shifts, query, onQuery)
    }
}

/** Отбор по виду документа: названия — от источника, а не свои. */
@Composable
private fun TypeChip(
    journal: HistoryJournalTexts,
    types: List<JournalType>,
    query: JournalQuery,
    onQuery: (JournalQuery) -> Unit
) {
    if (types.isEmpty()) return
    val title = { code: String? -> types.firstOrNull { it.code == code }?.title ?: journal.allTypes }
    ChipGroup(journal.documentType) {
        MenuChip(
            value = title(query.type),
            options = listOf(null) + types.map { it.code },
            title = title,
            chosen = query.type != null,
            onSelect = { onQuery(query.copy(type = it)) }
        )
    }
}

/** Отбор по состоянию доставки: доставлен, в очереди, отклонён. */
@Composable
private fun DeliveryChip(
    journal: HistoryJournalTexts,
    deliveries: List<JournalDelivery>,
    query: JournalQuery,
    onQuery: (JournalQuery) -> Unit
) {
    if (deliveries.size < 2) return
    val states = LocalStrings.current.status
    val title = { state: JournalDelivery? -> state?.title(states) ?: journal.allStates }
    ChipGroup(journal.deliveryState) {
        MenuChip(
            value = title(query.delivery),
            options = listOf(null) + deliveries,
            title = title,
            chosen = query.delivery != null,
            onSelect = { onQuery(query.copy(delivery = it)) }
        )
    }
}

/** Отбор по смене: за месяц их десятки. */
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

/** Подпись отбора и его плашка: отбор читается от своего названия. */
@Composable
private fun ChipGroup(title: String, chip: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        chip()
    }
}
