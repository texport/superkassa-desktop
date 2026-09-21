package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Кассы одного места списком.
 *
 * Стоит на месте карточки кассы, когда раскрыт ярлычок с числом: в этом
 * доме их несколько, и показывать карточку одной из них, выбранной
 * неизвестно по какому правилу, нельзя. Выбранная строка открывает
 * ту же карточку, что и одиночный ярлычок, — путь к кассе один.
 *
 * Список ограничен по высоте: место с двумя кассами не должно съедать
 * карту, а место с десятком — распирать экран.
 */
@Composable
fun AnalyticsSpotCard(
    group: KkmGroup,
    texts: AnalyticsTexts,
    onChoose: (PlacedKkm) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            SpotHead(group, texts)
            ScrollableList(modifier = Modifier.heightIn(max = Sizes.spotList)) {
                itemsIndexed(group.kkms, key = { _, row -> "spot-${row.kkm.cashRegisterId}" }) { at, row ->
                    RecordRow(
                        title = kkmTitle(row.kkm),
                        subtitle = row.kkm.retailPlaceName,
                        striped = at % 2 == 1,
                        onClick = { onChoose(row) }
                    )
                }
            }
        }
    }
}

/** Чем названо место: сколько здесь касс, чья точка и по какому адресу. */
@Composable
private fun SpotHead(group: KkmGroup, texts: AnalyticsTexts) {
    Text(
        text = "${texts.kkmsHere} · ${group.size}",
        style = MaterialTheme.typography.titleMedium
    )
    val about = listOfNotNull(group.place, group.address).joinToString(" · ")
    if (about.isNotBlank()) {
        Text(
            text = about,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
