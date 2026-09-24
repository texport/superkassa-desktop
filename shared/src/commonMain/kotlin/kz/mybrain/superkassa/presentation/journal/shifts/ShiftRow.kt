package kz.mybrain.superkassa.presentation.journal.shifts

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
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.journal.model.isClosed
import kz.mybrain.superkassa.domain.journal.model.zReportId
import kz.mybrain.superkassa.presentation.common.document.color
import kz.mybrain.superkassa.presentation.common.document.rowTint
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.list.stripedAt
import kz.mybrain.superkassa.presentation.common.period.text
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.common.table.ScrollingTable
import kz.mybrain.superkassa.presentation.common.table.TableColumn
import kz.mybrain.superkassa.presentation.common.table.TableLine
import kz.mybrain.superkassa.presentation.common.table.TableWidths
import kz.mybrain.superkassa.presentation.common.table.grownColumns
import kz.mybrain.superkassa.presentation.common.table.leastWidths
import kz.mybrain.superkassa.presentation.common.text.NumberText
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.size.HistoryLayout
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.theme.size.TableColumns
import kz.mybrain.superkassa.strings.api.journal.ShiftJournalTexts

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
 * ещё нет, и нажатие дало бы отказ кассы вместо бумаги.
 */
@Composable
internal fun ShiftTable(
    journal: ShiftJournalTexts,
    shifts: List<ShiftResponse>,
    modifier: Modifier,
    onOpen: (ShiftResponse) -> Unit,
    onZReport: (ShiftResponse) -> Unit
) {
    val columns = grownColumns(SHIFT_COLUMNS, setOf(Z_REPORT))
    ScrollingTable(
        columns = columns,
        // Таблица своей ширины и полосы прокрутки стоят у её края,
        // а не в тысяче точек от неё у края раздела.
        modifier = modifier.widthIn(max = leastWidths(columns).total + Spacing.scrollbarGutter),
        header = { widths -> ShiftHeader(journal, widths) },
        rows = { widths ->
            itemsIndexed(shifts) { at, shift ->
                val line = Modifier.heightIn(min = HistoryLayout.row)
                    .background(rowTint(stripedAt(at)))
                    .clickable { onOpen(shift) }
                ShiftRow(journal, shift, widths, line) { onZReport(shift) }
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
                    modifier = Modifier.padding(vertical = Spacing.itemGap)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Строка смены.
 *
 * @param line подложка и нажатие строки: смена открывается нажатием на неё.
 */
@Composable
private fun ShiftRow(
    journal: ShiftJournalTexts,
    shift: ShiftResponse,
    widths: TableWidths,
    line: Modifier,
    onZReport: () -> Unit
) {
    TableLine(widths, line) { column ->
        when (column) {
            NUMBER -> NumberText(shift.shiftNo.toString())
            OPENED -> NumberText(Dates.stamp(shift.openedAt))
            CLOSED -> if (shift.isClosed) {
                NumberText(Dates.stamp(shift.closedAt))
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
