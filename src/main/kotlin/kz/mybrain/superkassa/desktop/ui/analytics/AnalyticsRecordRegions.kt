package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Учёт по регионам: строка области и её числа.
 *
 * Столбцы те же, что и у свода выручки, и стоят они по тем же ширинам:
 * две таблицы одного раздела, разъехавшиеся столбцами, читаются как
 * два разных экрана. Название области тянется на всё остальное — оно
 * одно здесь непредсказуемой длины.
 *
 * Числа учёта покрашены так же, как плитки над таблицей: столбец
 * отказов должен находиться глазом сразу, не по заголовку.
 */
@Composable
internal fun RecordRegionsHead(texts: AnalyticsTexts) {
    TableRow {
        HeadCell(texts.sales.region, Modifier.weight(1f))
        HeadCell(texts.sales.placeCount, Modifier.width(Sizes.salesNumberColumn))
        HeadCell(texts.kkmCount, Modifier.width(Sizes.salesNumberColumn))
        KkmRecord.entries.forEach { meaning ->
            HeadCell(recordTitle(meaning, texts), Modifier.width(Sizes.salesNumberColumn))
        }
    }
}

/** Строка области: точки, кассы и разложение по смыслам учёта. */
@Composable
internal fun RecordRegionRow(region: RecordRegion) {
    val count = region.count
    TableRow(Modifier.padding(vertical = Spacing.tight)) {
        Text(
            text = region.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        RowCell(Money.count(count.places), Modifier.width(Sizes.salesNumberColumn))
        RowCell(Money.count(count.total), Modifier.width(Sizes.salesNumberColumn))
        // Столбцы смыслов идут тем же перечислением, что и подписи над
        // ними: порядок у них один, и разойтись ему негде.
        KkmRecord.entries.forEach { meaning -> NumberCell(count.of(meaning), recordTone(meaning)) }
    }
}

/** Число учёта в строке области: тем же цветом, что и плитка над столбцом. */
@Composable
private fun NumberCell(value: Int, tone: StatusTone) {
    RowCell(Money.count(value), Modifier.width(Sizes.salesNumberColumn), recordPaint(value, tone))
}
