package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.strings.ShiftJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Строка смены.
 *
 * Номер смены — заголовок, время открытия и закрытия — подпись под ним:
 * так строка читается как запись журнала, а не как ряд из трёх колонок,
 * в которых непонятно, что к чему относится.
 *
 * Кнопка Z-отчёта показана только у закрытой смены: у открытой отчёта
 * ещё нет, и нажатие дало бы отказ узла вместо бумаги.
 */
@Composable
internal fun ShiftRow(
    journal: ShiftJournalTexts,
    shift: Shift,
    striped: Boolean,
    onOpen: () -> Unit,
    onZReport: () -> Unit
) {
    RecordRow(
        title = "${journal.number} ${shift.shiftNo ?: Glyphs.DASH}",
        subtitle = shiftMoments(journal, shift),
        striped = striped,
        onClick = onOpen,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    text = if (shift.isClosed) journal.closed else journal.stillOpen,
                    color = if (shift.isClosed) StatusColors.delivered else StatusColors.pending
                )
                if (shift.zReportId != null) {
                    TextButton(onClick = onZReport) { Text(journal.zReport) }
                }
            }
        }
    )
}

/** Время смены одной строкой: открыта тогда-то, закрыта тогда-то. */
private fun shiftMoments(journal: ShiftJournalTexts, shift: Shift): String {
    val opened = "${journal.opened}: ${momentText(shift.openedAt)}"
    val closed = if (shift.isClosed) "${journal.closed}: ${momentText(shift.closedAt)}" else journal.stillOpen
    return "$opened${Glyphs.SEPARATOR}$closed"
}
