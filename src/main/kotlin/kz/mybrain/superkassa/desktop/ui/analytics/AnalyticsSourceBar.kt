package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.CounterTile
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Откуда брать положение касс — и что из этого вышло.
 *
 * Переключатель стоит над картой и он здесь главный: три его значения
 * отвечают на три разных вопроса владельца. Чем они отличаются друг
 * от друга, читают один раз при настройке — и потому объяснение живёт
 * под значком, а не абзацем под переключателем: места абзац занимал
 * всегда, а карте на экране его не хватает.
 *
 * Счётчики рядом — итог выбора: сколько касс встало на карту, а сколько
 * осталось без места. Это состояние, и оно остаётся на экране.
 */
@Composable
fun AnalyticsSourceBar(
    model: AnalyticsMapModel,
    placement: Placement,
    texts: AnalyticsTexts,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChoiceSegments(
            options = PositionSource.entries,
            selected = model.source,
            label = { sourceTitle(it, texts) },
            enabled = !model.loading,
            onSelect = { model.choose(it) }
        )
        InfoTip(sourceHint(model.source, texts))
        CounterTile(placement.placed.size.toString(), texts.placed)
        CounterTile(placement.unplaced.size.toString(), texts.withoutPosition)
        IconButton(onClick = onRefresh, enabled = !model.loading) {
            Icon(AppIcons.refresh, contentDescription = texts.refresh)
        }
    }
}
