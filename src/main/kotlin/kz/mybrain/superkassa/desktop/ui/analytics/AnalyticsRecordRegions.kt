package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.adaptive.TableColumn
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.recordTitle
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.AnalyticsLayout
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.TableColumns

/**
 * Учёт по регионам: строка области и её числа.
 *
 * Чисел здесь семь — больше, чем в любой другой таблице раздела. Прежде
 * столбцы делили ширину долями, и в узком окне на крупной ступени
 * у названия области и у подписей оставались обрывки: «Солтүстік Қаз…»,
 * «МКК-ға өтініш бе…». Теперь у каждого столбца своя наименьшая ширина:
 * название области встаёт целиком, подпись смысла — по словам в две-три
 * строки, а не хватает окна — таблица едет вбок.
 *
 * Числа учёта покрашены так же, как плитки над таблицей: столбец
 * отказов должен находиться глазом сразу, не по заголовку.
 */
@Composable
internal fun RecordRegionsHead(table: TableAcross, texts: AnalyticsTexts) {
    AcrossLine(table, Modifier.padding(vertical = Spacing.tight)) { column ->
        when (column) {
            NAME -> HeadCell(texts.sales.region)
            PLACES -> HeadCell(texts.sales.placeCount, MEANING_LINES, numeric = true)
            KKMS -> HeadCell(texts.kkmCount, MEANING_LINES, numeric = true)
            else -> HeadCell(recordTitle(meaningAt(column), texts.sieve), MEANING_LINES, numeric = true)
        }
    }
}

/** Строка области: точки, кассы и разложение по смыслам учёта. */
@Composable
internal fun RecordRegionRow(table: TableAcross, region: RecordRegion) {
    val count = region.count
    AcrossLine(table, Modifier.padding(vertical = Spacing.tight)) { column ->
        when (column) {
            NAME -> RowCell(region.title)
            PLACES -> CountCell(count.places)
            KKMS -> CountCell(count.total)
            // Столбцы смыслов идут тем же перечислением, что и подписи над
            // ними: порядок у них один, и разойтись ему негде.
            else -> meaningAt(column).let { CountCell(count.of(it), recordPaint(count.of(it), recordTone(it))) }
        }
    }
}

/** Смысл учёта в столбце: они идут за названием, точками и кассами. */
private fun meaningAt(column: Int): KkmRecord = KkmRecord.entries[column - MEANINGS]

private const val NAME = 0
private const val PLACES = 1
private const val KKMS = 2
private const val MEANINGS = 3

/** Сколько строк отведено подписи числа: «МКК-ға өтініш берілген» в одну не встаёт. */
private const val MEANING_LINES = 3

/** Область, точки, кассы и по столбцу на каждый смысл учёта. */
internal val REGION_COLUMNS: List<TableColumn> = listOf(
    TableColumn(min = AnalyticsLayout.regionName),
    TableColumn(min = TableColumns.count, weight = 0f, numeric = true),
    TableColumn(min = TableColumns.count, weight = 0f, numeric = true)
) + KkmRecord.entries.map { TableColumn(min = AnalyticsLayout.recordMeaning, weight = 0f, numeric = true) }
