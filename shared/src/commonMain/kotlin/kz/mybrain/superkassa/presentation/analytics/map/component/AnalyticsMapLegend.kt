package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.presentation.common.mapview.MapFold
import kz.mybrain.superkassa.presentation.common.section.Collapsible
import kz.mybrain.superkassa.presentation.common.section.SectionHeader
import kz.mybrain.superkassa.presentation.common.status.StatusTone
import kz.mybrain.superkassa.presentation.common.status.toneColor
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts

/**
 * Что означают цвета и размеры ярлычков — карточкой в углу карты.
 *
 * Стоит на своей поверхности поверх плиток, как и кнопки управления:
 * подпись без подложки на пёстрой карте не читается вовсе.
 */
@Composable
internal fun MapLegend(legend: MapFold, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(Sizes.mapNoteWidth),
        shape = RoundedCornerShape(Sizes.corner),
        tonalElevation = Sizes.dialogElevation,
        shadowElevation = Sizes.mapMarkLift
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.fieldGap, vertical = Spacing.itemGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.inline)
        ) {
            SectionHeader(texts.mapLegend, legend.expanded, legend::toggle)
            Collapsible(legend.expanded) { LegendLines(texts) }
        }
    }
}

/**
 * Строки легенды по порядку от спокойного к тревожному: глаз идёт сверху
 * вниз, и так он идёт от «всё хорошо» к «надо вмешаться».
 */
@Composable
private fun LegendLines(texts: AnalyticsTexts) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.inline)) {
        LegendLine(toneColor(StatusTone.Good), texts.legendGood)
        LegendLine(toneColor(StatusTone.Idle), texts.legendIdle)
        LegendLine(toneColor(StatusTone.Waiting), texts.legendSomeTrouble)
        LegendLine(toneColor(StatusTone.Bad), texts.legendTrouble)
        LegendNote(texts.legendSize)
        LegendNote(texts.legendChosen)
    }
}

/** Строка легенды: кружок того же цвета, каким покрашен ярлычок, и что он значит. */
@Composable
private fun LegendLine(tone: Color, words: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(Sizes.mapLegendDot),
            shape = CircleShape,
            color = tone,
            content = {}
        )
        Text(text = words, style = MaterialTheme.typography.labelMedium)
    }
}

/** Строка о размере и о выбранном: без кружка — цвета она не объясняет. */
@Composable
private fun LegendNote(words: String) {
    Text(
        text = words,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
