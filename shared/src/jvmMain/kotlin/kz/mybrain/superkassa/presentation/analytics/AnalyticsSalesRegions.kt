package kz.mybrain.superkassa.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kz.mybrain.superkassa.presentation.adaptive.TableColumn
import kz.mybrain.superkassa.presentation.adaptive.TableLine
import kz.mybrain.superkassa.presentation.adaptive.TableWidths
import kz.mybrain.superkassa.presentation.cabinet.cabinetSum
import kz.mybrain.superkassa.presentation.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.presentation.theme.AnalyticsLayout
import kz.mybrain.superkassa.presentation.theme.ChartColors
import kz.mybrain.superkassa.presentation.theme.Glyphs
import kz.mybrain.superkassa.presentation.theme.Sizes
import kz.mybrain.superkassa.presentation.theme.Spacing
import kz.mybrain.superkassa.presentation.theme.TableColumns

/**
 * Сеть по регионам: где она торгует и сколько это даёт.
 *
 * Разрез, которого не давали ни таблица касс, ни таблица точек: сотню
 * точек по областям глазами не сложить, а вопрос о картине по регионам
 * задают первым. Регионы идут по убыванию выручки — с них и начинают
 * читать, — и у каждого полоска доли сети: ряд полосок сравнивается
 * между собой быстрее, чем ряд процентов.
 *
 * Столбцы те же, что и у остальных таблиц сводки: числа стоят на месте
 * по своим ширинам и вправо, с разрядами, а название региона тянется
 * на всё остальное. Не хватает окна — таблица едет вбок, а сумма
 * не теряет хвост за краем столбца.
 */
@Composable
fun SalesRegions(regions: List<SalesRegion>, texts: AnalyticsSalesTexts, modifier: Modifier = Modifier) {
    if (regions.isEmpty()) {
        Footnote(texts.empty)
        return
    }
    PageTable(COLUMNS, modifier) { table ->
        RegionsHead(table.widths, texts)
        regions.forEach { region ->
            LineDivider(table.widths)
            RegionRow(table.widths, region)
        }
    }
}

/** Подписи столбцов свода. */
@Composable
private fun RegionsHead(widths: TableWidths, texts: AnalyticsSalesTexts) {
    TableLine(widths, Modifier.padding(vertical = Spacing.tight)) { column ->
        when (column) {
            NAME -> HeadCell(texts.region)
            PLACES -> HeadCell(texts.placeCount, numeric = true)
            KKMS -> HeadCell(texts.activeRegisters, numeric = true)
            RECEIPTS -> HeadCell(texts.receipts, numeric = true)
            REVENUE -> HeadCell(texts.revenue, numeric = true)
            else -> HeadCell(texts.networkShare)
        }
    }
}

/** Строка свода: регион, его числа и доля сети полоской. */
@Composable
private fun RegionRow(widths: TableWidths, region: SalesRegion) {
    TableLine(widths, Modifier.padding(vertical = Spacing.tight)) { column ->
        when (column) {
            NAME -> RowCell(region.title)
            PLACES -> CountCell(region.placeCount)
            KKMS -> CountCell(region.registerCount)
            RECEIPTS -> CountCell(region.receiptCount)
            REVENUE -> SumCell(cabinetSum(region.revenue))
            else -> RegionShare(region.percent)
        }
    }
}

private const val NAME = 0
private const val PLACES = 1
private const val KKMS = 2
private const val RECEIPTS = 3
private const val REVENUE = 4

/** Регион, точки, кассы на связи, чеки, выручка и доля сети. */
private val COLUMNS = listOf(
    TableColumn(min = AnalyticsLayout.regionName, weight = NAME_SHARE),
    TableColumn(min = TableColumns.count, numeric = true),
    TableColumn(min = TableColumns.count, numeric = true),
    TableColumn(min = TableColumns.count, numeric = true),
    TableColumn(min = AnalyticsLayout.networkSum, numeric = true),
    TableColumn(min = Sizes.salesShareColumn, weight = 0f)
)

/** Название области берёт вдвое больше лишнего места, чем каждое число. */
private const val NAME_SHARE = 2f

/**
 * Доля региона в выручке сети: полоска и процент рядом с ней.
 *
 * Полоска идёт от общей длины столбца, а не от самой крупной доли:
 * десять процентов должны выглядеть десятью процентами, а не половиной
 * полоски потому, что первый регион взял пятую часть сети.
 */
@Composable
private fun RegionShare(percent: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(Sizes.shareBar)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            // Пустая доля не рисуется вовсе: нулевой множитель ширины
            // Compose не берёт, а полоска в волосок читалась бы как доля.
            if (percent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent / PERCENT)
                        .fillMaxHeight()
                        .background(ChartColors.bar)
                )
            }
        }
        Text(
            text = "$percent${Glyphs.NBSP}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** Целое в процентах: доля полоски меряется им же. */
private const val PERCENT = 100f
