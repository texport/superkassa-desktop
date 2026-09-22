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
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
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
 *
 * @param expanded развёрнута ли карточка; свёрнутая остаётся заголовком.
 * @param onToggle сворачивание; без него стрелки у заголовка нет.
 */
@Composable
fun AnalyticsSpotCard(
    group: KkmGroup,
    texts: AnalyticsTexts,
    cabinet: CabinetTexts,
    onChoose: (PlacedKkm) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            MapCardTitle("${texts.kkmsHere} · ${group.size}", expanded, onToggle)
            MapCardBody(expanded) {
                SpotAbout(group)
                SpotRows(group, texts, cabinet, onChoose)
            }
        }
    }
}

/** Чьё это место: торговая точка и адрес. Ничего не известно — строки нет. */
@Composable
private fun SpotAbout(group: KkmGroup) {
    val about = listOfNotNull(group.place, group.address).joinToString(" · ")
    if (about.isBlank()) return
    Text(
        text = about,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/** Кассы места строками; выбранная открывает свою карточку. */
@Composable
private fun SpotRows(group: KkmGroup, texts: AnalyticsTexts, cabinet: CabinetTexts, onChoose: (PlacedKkm) -> Unit) {
    ScrollableList(modifier = Modifier.heightIn(max = Sizes.spotList)) {
        itemsIndexed(group.kkms, key = { _, row -> "spot-${row.kkm.cashRegisterId}" }) { at, row ->
            RecordRow(
                title = kkmTitle(row.kkm),
                // Торговая точка у всех строк места одна и та же —
                // она сказана в шапке. Под названием стоит то, чем
                // кассы места и различаются: состояние и смена.
                support = { KkmChips(row.kkm, texts, cabinet) },
                striped = at % 2 == 1,
                onClick = { onChoose(row) }
            )
        }
    }
}
