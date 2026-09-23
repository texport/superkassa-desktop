package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.adaptive.MoneyText
import kz.mybrain.superkassa.desktop.ui.adaptive.NumberText
import kz.mybrain.superkassa.desktop.ui.adaptive.ScrollingTable
import kz.mybrain.superkassa.desktop.ui.adaptive.TableColumn
import kz.mybrain.superkassa.desktop.ui.adaptive.TableLine
import kz.mybrain.superkassa.desktop.ui.adaptive.TableWidths
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.HistoryLayout
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.TableColumns

/**
 * Таблица журнала документов: столбцы, шапка и строки.
 *
 * Одна на журнал кассы, документы смены и документы кассы в кабинете:
 * столбцы и место кнопок заданы здесь, а не на каждом экране. Написанные
 * дважды, они разъезжались — сумма стояла в разных столбцах у одного чека.
 *
 * Столбец не бывает уже своей наименьшей ширины: кнопка печати пропадала
 * до точки, сумма слипалась с признаком. Теперь таблица едет вбок.
 */
@Composable
fun JournalTable(
    journal: HistoryJournalTexts,
    entries: List<JournalEntry>,
    modifier: Modifier = Modifier,
    onOpen: ((JournalEntry) -> Unit)? = null,
    onPreview: ((JournalEntry) -> Unit)? = null,
    onPrint: ((JournalEntry) -> Unit)? = null
) {
    ScrollingTable(
        columns = journalColumns(),
        modifier = modifier,
        header = { widths -> JournalHeader(journal, widths) },
        rows = { widths ->
            itemsIndexed(entries, key = { _, entry -> entry.key }) { at, entry ->
                JournalRow(
                    entry = entry,
                    striped = stripedAt(at),
                    onOpen = onOpen?.let { open -> { open(entry) } },
                    onPreview = onPreview?.let { preview -> { preview(entry) } },
                    onPrint = onPrint?.let { print -> { print(entry) } },
                    widths = widths
                )
            }
        }
    )
}

/** Подписи столбцов: мельче и тише строки, иначе шапка спорит с данными. */
@Composable
fun JournalHeader(journal: HistoryJournalTexts, widths: TableWidths = leastWidths(journalColumns())) {
    val titles = listOf(
        journal.colTime, journal.colType, journal.colNumber, journal.colShift,
        journal.colAmount, journal.colFiscalSign, LocalStrings.current.dashboard.state
    )
    Column {
        TableLine(widths, Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) { column ->
            // Над кнопками подписи нет: значки говорят за себя, а слово
            // «Печать» над двумя кнопками обрезалось бы до «Печ…».
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

/**
 * Строка журнала.
 *
 * Карточки под каждой строкой не осталось намеренно: они разносили сотню
 * чеков на три экрана прокрутки и рвали столбец сумм зазорами. Строки идут
 * вплотную, а разделяет их подложка через одну.
 *
 * @param onOpen что показать по нажатию на строку; `null` — строка
 *   не нажимается, и указатель это показывает.
 * @param onPreview показ печатной формы; `null` — формы у источника нет.
 * @param onPrint отправка формы на принтер; `null` — печатать нечем.
 * @param widths ширины столбцов в месте таблицы.
 */
@Composable
fun JournalRow(
    entry: JournalEntry,
    striped: Boolean,
    onOpen: (() -> Unit)? = null,
    onPreview: (() -> Unit)? = null,
    onPrint: (() -> Unit)? = null,
    widths: TableWidths = leastWidths(journalColumns())
) {
    // Строка своей ширины и там, где места меньше: столбцы не сжимаются,
    // а выходят за край — в таблице их приводит прокрутка вбок.
    val base = Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
        .heightIn(min = HistoryLayout.row)
        .background(rowTint(striped))
    TableLine(widths, if (onOpen == null) base else base.clickable(onClick = onOpen)) { column ->
        when (column) {
            TIME -> NumberText(entry.moment)
            TYPE -> NameCell(entry.type)
            NUMBER -> NumberText(entry.number)
            SHIFT -> NumberText(entry.shiftNo?.toString() ?: Glyphs.DASH)
            AMOUNT -> MoneyText(entry.amount)
            SIGN -> NumberText(entry.sign)
            STATE -> StateCell(entry)
            PREVIEW -> RowAction(entry, onPreview, preview = true)
            else -> RowAction(entry, onPrint, preview = false)
        }
    }
}

private const val TIME = 0
private const val TYPE = 1
private const val NUMBER = 2
private const val SHIFT = 3
private const val AMOUNT = 4
private const val SIGN = 5
private const val STATE = 6
private const val PREVIEW = 7
private const val PRINT = 8

/**
 * Столбцы журнала по порядку; растут вместе с размером шрифта.
 *
 * Лишнее место уходит виду документа, сумме и состоянию: у суммы в широком
 * окне — своя ступень вместо уменьшенной, у вида и состояния — надписи,
 * по-казахски длиннее всего. Номера и кнопки своей ширины всегда.
 */
@Composable
private fun journalColumns(): List<TableColumn> = grownColumns(JOURNAL_COLUMNS, setOf(PREVIEW, PRINT))

private val JOURNAL_COLUMNS = listOf(
    TableColumn(TableColumns.moment, weight = 0f),
    TableColumn(TableColumns.name, weight = 1f),
    TableColumn(TableColumns.number, weight = 0f, numeric = true),
    TableColumn(TableColumns.number, weight = 0f, numeric = true),
    TableColumn(TableColumns.money, weight = 1f, numeric = true),
    TableColumn(TableColumns.number, weight = 0f, numeric = true),
    TableColumn(HistoryLayout.deliveryStatus, weight = 1f),
    TableColumn(TableColumns.action, weight = 0f),
    TableColumn(TableColumns.action, weight = 0f)
)
