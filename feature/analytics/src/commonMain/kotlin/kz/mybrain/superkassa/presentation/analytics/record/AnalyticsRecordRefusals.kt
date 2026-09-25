package kz.mybrain.superkassa.presentation.analytics.record

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.status.StatusTone
import kz.mybrain.superkassa.designsystem.status.toneColor
import kz.mybrain.superkassa.designsystem.table.TableColumn
import kz.mybrain.superkassa.designsystem.text.NumberText
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.size.TableColumns
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.presentation.analytics.common.AcrossLine
import kz.mybrain.superkassa.presentation.analytics.common.HeadCell
import kz.mybrain.superkassa.presentation.analytics.common.RowCell
import kz.mybrain.superkassa.presentation.analytics.common.TableAcross
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts

/**
 * Кассы, которым КГД отказал в учёте.
 *
 * Единственное на вкладке, по чему владелец действует руками: отказ
 * сам не пройдёт, заявление нужно разобрать и подать заново. Потому
 * отказы стоят выше разреза по регионам — до них доходят глазами
 * раньше, чем до областей.
 *
 * В строке то, чем касса опознаётся в заявлении и на месте: своё
 * название, номер КГД, торговая точка с адресом и время последней связи.
 * Причины отказа в ответе кабинета нет — её читают в самом заявлении,
 * и выдумывать её здесь нельзя.
 */
@Composable
internal fun RecordRefusalsHead(table: TableAcross, texts: AnalyticsTexts) {
    AcrossLine(table, Modifier.padding(vertical = Spacing.itemGap)) { column ->
        when (column) {
            KKM -> HeadCell(texts.kkmColumn)
            NUMBER -> HeadCell(texts.registrationNumber, numeric = true)
            PLACE -> HeadCell(texts.retailPlace)
            else -> HeadCell(texts.lastContact)
        }
    }
}

/**
 * Строка отказа: касса, её номер, где она стоит и когда выходила на связь.
 *
 * Номер КГД — числом целиком: по нему кассу ищут в заявлении, и номер
 * с многоточием вместо последних цифр искать бесполезно.
 */
@Composable
internal fun RecordRefusalRow(table: TableAcross, kkm: AnalyticsKkm) {
    AcrossLine(table, Modifier.padding(vertical = Spacing.itemGap)) { column ->
        when (column) {
            KKM -> RowCell(kkm.title, toneColor(StatusTone.Bad))
            NUMBER -> NumberText(kkm.registrationNumber ?: Glyphs.DASH)
            PLACE -> RowCell(placeWords(kkm))
            else -> RowCell(Dates.momentOf(kkm.lastContactAt))
        }
    }
}

private const val KKM = 0
private const val NUMBER = 1
private const val PLACE = 2

/** Касса, номер КГД, торговая точка с адресом и последняя связь. */
internal val REFUSAL_COLUMNS = listOf(
    TableColumn(min = TableColumns.name),
    TableColumn(min = TableColumns.number, weight = 0f, numeric = true),
    TableColumn(min = TableColumns.name, weight = 2f),
    TableColumn(min = TableColumns.moment, weight = 0f)
)

/**
 * Где стоит эта касса — точкой и адресом в одной клетке.
 *
 * Врозь они заняли бы два столбца из четырёх, а читают их вместе:
 * «Магазин на Абая» без адреса не отличить от такого же в соседнем
 * городе, а адрес без названия точки владелец не узнаёт.
 */
private fun placeWords(kkm: AnalyticsKkm): String = listOfNotNull(
    kkm.retailPlaceName?.takeIf(String::isNotBlank),
    kkm.address?.takeIf(String::isNotBlank)
).joinToString(Glyphs.SEPARATOR).ifBlank { Glyphs.DASH }
