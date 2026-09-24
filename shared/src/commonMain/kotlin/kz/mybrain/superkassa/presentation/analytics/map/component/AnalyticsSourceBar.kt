package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.analytics.model.Placement
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.placement
import kz.mybrain.superkassa.presentation.analytics.common.sourceHint
import kz.mybrain.superkassa.presentation.analytics.common.sourceTitle
import kz.mybrain.superkassa.presentation.analytics.map.AnalyticsMapActions
import kz.mybrain.superkassa.presentation.analytics.map.AnalyticsMapUiState
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.common.picker.ChoiceSegments
import kz.mybrain.superkassa.presentation.common.section.CounterTile
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Откуда брать положение касс — и что из этого вышло.
 *
 * Переключатель стоит над картой и он здесь главный: три его значения
 * отвечают на три разных вопроса владельца. Чем они отличаются друг
 * от друга, читают один раз при настройке — и потому объяснение живёт
 * под значком, а не абзацем под переключателем: места абзац занимал
 * всегда, а карте на экране его не хватает.
 *
 * Счётчики рядом — итог выбора: сколько касс встало на карту, сколько
 * ещё ищется и сколько осталось без места. Это состояние, и оно
 * остаётся на экране.
 *
 * Ряд переносится, а не сжимается: в узком окне и на казахском
 * «Обновить» уходило за край, а сегменты сжимались до обрывков слов.
 *
 * Ищущиеся считаются своим числом и только пока они есть. При адресе
 * торговой точки координат кабинет не даёт вовсе, и дома карта находит
 * по одному: сведённые с непоставленными, они писали бы «без положения»
 * о трёх тысячах касс, у каждой из которых адрес есть.
 */
@Composable
fun AnalyticsSourceBar(
    model: AnalyticsMapUiState,
    placement: Placement,
    texts: AnalyticsTexts,
    actions: AnalyticsMapActions
) {
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.cardGap) {
        ChoiceSegments(
            options = PositionSource.entries,
            selected = model.source,
            label = { sourceTitle(it, texts) },
            enabled = !model.reading.loading,
            onSelect = actions::choose
        )
        InfoTip(sourceHint(model.source, texts))
        CounterTile(Money.count(placement.placed.size), texts.placed)
        if (placement.searching > 0) CounterTile(Money.count(placement.searching), texts.searchingCount)
        CounterTile(Money.count(placement.nowhere), texts.withoutPosition)
        IconButton(onClick = actions::refresh, enabled = !model.reading.loading) {
            Icon(AppIcons.refresh, contentDescription = texts.refresh)
        }
    }
}
