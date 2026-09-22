package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetSum
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.theme.ChartColors
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
 * по своим ширинам, а название региона тянется на всё остальное.
 */
@Composable
fun SalesRegions(regions: List<SalesRegion>, texts: AnalyticsSalesTexts, modifier: Modifier = Modifier) {
    if (regions.isEmpty()) {
        Footnote(texts.empty)
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        RegionsHead(texts)
        regions.forEach { region ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RegionRow(region)
        }
    }
}

/** Подписи столбцов свода. */
@Composable
private fun RegionsHead(texts: AnalyticsSalesTexts) {
    TableRow {
        HeadCell(texts.region, Modifier.weight(1f))
        HeadCell(texts.placeCount, Modifier.width(Sizes.salesNumberColumn))
        HeadCell(texts.activeRegisters, Modifier.width(Sizes.salesNumberColumn))
        HeadCell(texts.receipts, Modifier.width(Sizes.salesNumberColumn))
        HeadCell(texts.revenue, Modifier.width(Sizes.salesNumberColumn))
        HeadCell(texts.networkShare, Modifier.width(Sizes.salesShareColumn))
    }
}

/** Строка свода: регион, его числа и доля сети полоской. */
@Composable
private fun RegionRow(region: SalesRegion) {
    TableRow(Modifier.padding(vertical = Spacing.tight)) {
        RowCell(region.title, Modifier.weight(1f))
        RowCell(region.placeCount.toString(), Modifier.width(Sizes.salesNumberColumn))
        RowCell(region.registerCount.toString(), Modifier.width(Sizes.salesNumberColumn))
        RowCell(region.receiptCount.toString(), Modifier.width(Sizes.salesNumberColumn))
        Text(
            text = cabinetSum(region.revenue),
            style = MoneyStyle.caption,
            maxLines = 1,
            modifier = Modifier.width(Sizes.salesNumberColumn)
        )
        RegionShare(region.percent)
    }
}

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
        modifier = Modifier.width(Sizes.salesShareColumn),
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
