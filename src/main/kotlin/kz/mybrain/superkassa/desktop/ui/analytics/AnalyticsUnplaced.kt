package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.components.SectionTitle
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Кассы, которых на карте нет.
 *
 * Стоят рядом с картой, а не исчезают из неё. Касса без адреса или
 * без координат — не отсутствующая касса, а незаконченная работа
 * владельца, и увидеть её он должен ровно там, где ищет свои кассы.
 * У каждой сказано, почему её не поставить: причины разные, и чинятся
 * они по-разному.
 */
@Composable
fun AnalyticsUnplaced(
    rows: List<UnplacedKkm>,
    source: PositionSource,
    texts: AnalyticsTexts,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        SectionTitle("${texts.withoutPosition} · ${rows.size}")
        if (rows.isEmpty()) {
            EmptyState(
                icon = AppIcons.place,
                title = texts.withoutPositionEmpty,
                hint = texts.withoutPositionEmptyHint,
                dense = true
            )
            return@Column
        }
        ScrollableList(modifier = Modifier.weight(1f)) {
            items(rows, key = { it.kkm.cashRegisterId }) { row ->
                UnplacedRow(row, source, texts)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/** Строка списка: название кассы, её торговая точка и причина под ними. */
@Composable
private fun UnplacedRow(row: UnplacedKkm, source: PositionSource, texts: AnalyticsTexts) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        Text(
            text = kkmTitle(row.kkm),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        row.kkm.retailPlaceName?.takeIf { it.isNotBlank() }?.let { place ->
            Text(
                text = place,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = troubleWords(row.reason, source, texts),
            style = MaterialTheme.typography.labelMedium,
            color = toneColor(troubleTone(row.reason))
        )
    }
}
