package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.analytics.map.component.AnalyticsKkmList
import kz.mybrain.superkassa.presentation.analytics.map.component.MapCardTitle
import kz.mybrain.superkassa.presentation.common.format.Money

/**
 * Список всех касс, а не только непоставленных: точки на карте
 * неотличимы, и владелец сети искал свою кассу глазами.
 *
 * Список — сворачиваемой карточкой, как карточка выбранной кассы под ним:
 * владелец, который ищет кассу на самой карте, отдаёт высоту карточке.
 */
@Composable
internal fun KkmList(parts: MapParts, modifier: Modifier = Modifier) {
    val fold = parts.tools.list
    val count = parts.placement.placed.size + parts.placement.unplaced.size
    OutlinedCard(modifier = if (fold.expanded) modifier.fillMaxSize() else modifier.fillMaxWidth()) {
        // Поля сверху и снизу тоньше, чем у карточки кассы: в низком окне
        // список и так делит высоту с картой и карточкой, и каждая строка
        // списка на счету.
        Column(
            modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.itemGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.inline)
        ) {
            MapCardTitle("${parts.texts.kkmCount} · ${Money.count(count)}", fold.expanded, fold::toggle)
            if (fold.expanded) {
                AnalyticsKkmList(
                    placed = parts.placement.placed,
                    unplaced = parts.placement.unplaced,
                    chosen = parts.state.chosen,
                    source = parts.state.source,
                    texts = parts.texts,
                    onChoose = { row -> parts.actions.show(row, parts.groups) },
                    modifier = Modifier.weight(1f),
                    sieved = parts.state.sieve.set
                )
            }
        }
    }
}
