package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.adaptive.NumberText
import kz.mybrain.superkassa.desktop.ui.adaptive.ScrollingTable
import kz.mybrain.superkassa.desktop.ui.adaptive.TableColumn
import kz.mybrain.superkassa.desktop.ui.adaptive.TableLine
import kz.mybrain.superkassa.desktop.ui.adaptive.TableWidths
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.strings.ShiftJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.HistoryLayout
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors
import kz.mybrain.superkassa.desktop.ui.theme.TableColumns

/**
 * Прошлые смены таблицей: номер, открытие, закрытие, Z-отчёт.
 *
 * Строкой списка смена растягивалась на всю ширину монитора: номер
 * у левого края, плашка и кнопка — в полутора тысячах точек от него,
 * а «Не закрыта» стояло и в подписи, и на плашке. В таблице столбцы
 * своей ширины, время открытия и закрытия стоят друг под другом у всех
 * смен, а у открытой смены на месте времени закрытия — плашка
 * «Не закрыта», одна на строку.
 *
 * Кнопка Z-отчёта показана только у закрытой смены: у открытой отчёта
 * ещё нет, и нажатие дало бы отказ узла вместо бумаги.
 */
@Composable
internal fun ShiftTable(
    journal: ShiftJournalTexts,
    shifts: List<Shift>,
    modifier: Modifier,
    onOpen: (Shift) -> Unit,
    onZReport: (Shift) -> Unit
) {
    val columns = grownColumns(SHIFT_COLUMNS, setOf(Z_REPORT))
    ScrollingTable(
        columns = columns,
        // Таблица своей ширины и полосы прокрутки стоят у её края,
        // а не в тысяче точек от неё у края раздела.
        modifier = modifier.widthIn(max = leastWidths(columns).total + Spacing.normal),
        header = { widths -> ShiftHeader(journal, widths) },
        rows = { widths ->
            itemsIndexed(shifts) { at, shift ->
                ShiftRow(journal, shift, stripedAt(at), widths, { onOpen(shift) }) { onZReport(shift) }
            }
        }
    )
}

@Composable
private fun ShiftHeader(journal: ShiftJournalTexts, widths: TableWidths) {
    val titles = listOf(journal.number, journal.opened, journal.closed)
    Column {
        TableLine(widths, Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) { column ->
            titles.getOrNull(column)?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = Spacing.tight)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ShiftRow(
    journal: ShiftJournalTexts,
    shift: Shift,
    striped: Boolean,
    widths: TableWidths,
    onOpen: () -> Unit,
    onZReport: () -> Unit
) {
    val line = Modifier.heightIn(min = HistoryLayout.row).background(rowTint(striped)).clickable(onClick = onOpen)
    TableLine(widths, line) { column ->
        when (column) {
            NUMBER -> NumberText(shift.shiftNo?.toString() ?: Glyphs.DASH)
            OPENED -> NumberText(momentText(shift.openedAt))
            CLOSED -> if (shift.isClosed) {
                NumberText(momentText(shift.closedAt))
            } else {
                Chip(journal.stillOpen, StatusColors.pending, MaterialTheme.typography.labelSmall)
            }

            else -> if (shift.zReportId != null) {
                TextButton(onClick = onZReport) { Text(journal.zReport, maxLines = 1) }
            }
        }
    }
}

private const val NUMBER = 0
private const val OPENED = 1
private const val CLOSED = 2
private const val Z_REPORT = 3

/**
 * Все столбцы своей ширины: таблица смен не тянется на весь монитор.
 * Столбец Z-отчёта уже рассчитан на крупную ступень и с ней не растёт.
 */
private val SHIFT_COLUMNS = listOf(
    TableColumn(TableColumns.number, weight = 0f, numeric = true),
    TableColumn(TableColumns.moment, weight = 0f),
    TableColumn(TableColumns.moment, weight = 0f),
    TableColumn(HistoryLayout.wordAction, weight = 0f)
)
