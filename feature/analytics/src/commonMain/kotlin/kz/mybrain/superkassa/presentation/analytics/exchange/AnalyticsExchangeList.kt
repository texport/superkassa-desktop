package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.table.ScrollingTable
import kz.mybrain.superkassa.designsystem.table.TableColumn
import kz.mybrain.superkassa.designsystem.table.TableLine
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.size.TableColumns
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.presentation.analytics.common.HeadCell
import kz.mybrain.superkassa.presentation.analytics.common.RowCell
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts

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
internal fun AnalyticsExchangeList(rows: List<ExchangeAddress>, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    ScrollingTable(
        columns = exchangeColumns(rows),
        modifier = modifier,
        header = { widths ->
            TableLine(widths, Modifier.padding(vertical = Spacing.itemGap)) { column ->
                HeadCell(headTitle(column, texts))
            }
        },
        rows = { widths ->
            items(rows, key = { "${it.cashRegisterId}/${it.address}" }) { row ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TableLine(
                    widths,
                    Modifier.padding(vertical = Spacing.itemGap)
                ) { column -> RowCell(cellText(column, row)) }
            }
        }
    )
}

/**
 * Адрес, касса, первый и последний обмен. Адрес и время — не уже своих
 * значений, см. [fitted]; касса тянется по месту.
 */
@Composable
private fun exchangeColumns(rows: List<ExchangeAddress>): List<TableColumn> {
    val address = fitted(rows.map { it.address }, Sizes.exchangeAddressColumn)
    val moments = rows.flatMap { listOf(Dates.momentOf(it.firstSeen), Dates.momentOf(it.lastSeen)) }
    val moment = fitted(moments, TableColumns.moment)
    return listOf(
        TableColumn(min = address),
        TableColumn(min = TableColumns.name, weight = NAME_WEIGHT),
        TableColumn(min = moment, weight = 0f),
        TableColumn(min = moment, weight = 0f)
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
    KKM -> row.title
    FIRST -> Dates.momentOf(row.firstSeen)
    else -> Dates.momentOf(row.lastSeen)
}

/** Сколько самых длинных значений мерится: узкие знаки делают длиннейшее по счёту не всегда широчайшим. */
private const val MEASURED = 16

/** Касса тянется вдвое против адреса: название длиннее. */
private const val NAME_WEIGHT = 2f

private const val ADDRESS = 0
private const val KKM = 1
private const val FIRST = 2

/**
 * Наименьшая ширина столбца: самое длинное его значение целиком.
 *
 * Адрес — то, ради чего таблица открыта, и обрывать его нельзя: IPv6
 * длиной в 39 знаков в окне 960 и 1180 обрывался посреди группы цифр —
 * «2a02:2168:a0f:0000:9c5d…», а на крупной ступени шрифта и время
 * обмена — «01.09.2026 1…». Ширина берётся по набранному тексту, а не
 * числом: ступень шрифта меряется тем же правилом. Не поместилась
 * таблица — она едет вбок, а значения не режутся.
 *
 * @param least ширина, уже которой столбец не бывает и при коротких значениях.
 */
@Composable
private fun fitted(values: List<String>, least: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val widest = remember(values, style, density) {
        // Меряются самые длинные по числу знаков: мерить каждую из тысяч строк незачем.
        values.distinct().sortedByDescending { it.length }.take(MEASURED)
            .maxOfOrNull { measurer.measure(it, style, softWrap = false).size.width } ?: 0
    }
    return max(least, with(density) { widest.toDp() } + TableColumns.cellPadding * 2)
}
