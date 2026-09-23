package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress
import kz.mybrain.superkassa.desktop.ui.adaptive.ScrollingTable
import kz.mybrain.superkassa.desktop.ui.adaptive.TableColumn
import kz.mybrain.superkassa.desktop.ui.adaptive.TableLine
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMoment
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.TableColumns

/**
 * Адреса обмена столбцами.
 *
 * Время стоит на месте, а адрес и касса тянутся: адреса и время сверяют
 * глазами сверху вниз, а колонка, гуляющая от строки к строке, читается
 * только построчно. Прежде адрес был постоянной ширины и на широком окне
 * обрывался многоточием рядом с пустым местом; теперь он растёт вместе
 * с окном, а в узком таблица едет вбок, а не сжимает столбцы.
 *
 * Порядок строк оставлен кабинетным — по убыванию последнего обмена:
 * свежая связь важнее всего остального, и сортировать его заново здесь
 * значило бы спорить с тем, кто знает о времени больше.
 */
@Composable
fun AnalyticsExchangeList(rows: List<ExchangeAddress>, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    ScrollingTable(
        columns = COLUMNS,
        modifier = modifier,
        header = { widths ->
            TableLine(widths, Modifier.padding(vertical = Spacing.tight)) { column ->
                HeadCell(headTitle(column, texts))
            }
        },
        rows = { widths ->
            items(rows, key = { "${it.cashRegisterId}/${it.address}" }) { row ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TableLine(widths, Modifier.padding(vertical = Spacing.tight)) { column -> RowCell(cellText(column, row)) }
            }
        }
    )
}

/** Подпись столбца по его номеру. */
private fun headTitle(column: Int, texts: AnalyticsTexts): String = when (column) {
    ADDRESS -> texts.exchangeAddress
    KKM -> texts.kkmColumn
    FIRST -> texts.firstSeen
    else -> texts.lastSeen
}

/** Клетка строки по номеру столбца: адрес, касса и два времени. */
private fun cellText(column: Int, row: ExchangeAddress): String = when (column) {
    ADDRESS -> row.address
    KKM -> kkmTitle(row)
    FIRST -> cabinetMoment(row.firstSeen)
    else -> cabinetMoment(row.lastSeen)
}

private const val ADDRESS = 0
private const val KKM = 1
private const val FIRST = 2

/** Адрес, касса, первый и последний обмен. */
private val COLUMNS = listOf(
    TableColumn(min = Sizes.exchangeAddressColumn),
    TableColumn(min = TableColumns.name, weight = 2f),
    TableColumn(min = TableColumns.moment, weight = 0f),
    TableColumn(min = TableColumns.moment, weight = 0f)
)
