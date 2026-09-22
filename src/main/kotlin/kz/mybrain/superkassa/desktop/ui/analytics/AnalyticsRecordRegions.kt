package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Учёт по регионам: строка области и её числа.
 *
 * Столбцы делят ширину долями, а не стоят по заданным ширинам. Чисел
 * здесь восемь — больше, чем в любой другой таблице раздела, — и восемь
 * столбцов по сто тридцать точек в окне тысячу шириной не оставляли
 * названию области ничего: строки начинались с числа точек, а какой
 * области они принадлежат, экран не говорил вовсе. Доли выдерживают
 * и узкое окно, и широкое, а строки всё так же стоят под своими
 * подписями — доли у них одни и те же.
 *
 * Названию области отведено вдвое против числа: оно одно здесь
 * непредсказуемой длины. Подписи чисел берут две строки — в узком окне
 * «Заявление в КГД» одной не умещается.
 *
 * Числа учёта покрашены так же, как плитки над таблицей: столбец
 * отказов должен находиться глазом сразу, не по заголовку.
 */
@Composable
internal fun RecordRegionsHead(texts: AnalyticsTexts) {
    TableRow {
        HeadCell(texts.sales.region, Modifier.weight(NAME_SHARE), HEAD_LINES)
        HeadCell(texts.sales.placeCount, Modifier.weight(1f), HEAD_LINES)
        HeadCell(texts.kkmCount, Modifier.weight(1f), HEAD_LINES)
        KkmRecord.entries.forEach { meaning ->
            HeadCell(recordTitle(meaning, texts), Modifier.weight(1f), HEAD_LINES)
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
            modifier = Modifier.weight(NAME_SHARE)
        )
        RowCell(Money.count(count.places), Modifier.weight(1f))
        RowCell(Money.count(count.total), Modifier.weight(1f))
        // Столбцы смыслов идут тем же перечислением, что и подписи над
        // ними: порядок у них один, и разойтись ему негде.
        KkmRecord.entries.forEach { meaning -> NumberCell(count.of(meaning), recordTone(meaning)) }
    }
}

/** Число учёта в строке области: тем же цветом, что и плитка над столбцом. */
@Composable
private fun RowScope.NumberCell(value: Int, tone: StatusTone) {
    RowCell(Money.count(value), Modifier.weight(1f), recordPaint(value, tone))
}

/** Во сколько раз название области шире одного числового столбца. */
private const val NAME_SHARE = 2f

/** Сколько строк отведено подписи столбца: в узком окне одной мало. */
private const val HEAD_LINES = 2
