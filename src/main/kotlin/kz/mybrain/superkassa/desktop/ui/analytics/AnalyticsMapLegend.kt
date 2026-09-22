package kz.mybrain.superkassa.desktop.ui.analytics

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.components.Collapsible
import kz.mybrain.superkassa.desktop.ui.components.SectionHeader
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Развёрнута ли легенда карты.
 *
 * Цвет ярлычка несёт смысл, и без подписи его приходится угадывать:
 * красный кружок на карте страны владелец читал то как «сломано»,
 * то как «много касс». Прочитав легенду один раз, он её сворачивает —
 * и повторять это при каждом открытии раздела не должен, как и с карточкой
 * под картой (см. [AnalyticsMapCard]).
 */
class AnalyticsMapLegend(private val preferences: Preferences) {

    var expanded: Boolean by mutableStateOf(!preferences.mapLegendCollapsed)
        private set

    /** Сворачивает развёрнутую легенду и наоборот; выбор запоминается. */
    fun toggle() {
        expanded = !expanded
        preferences.mapLegendCollapsed = !expanded
    }
}

/**
 * Что означают цвета и размеры ярлычков — карточкой в углу карты.
 *
 * Стоит на своей поверхности поверх плиток, как и кнопки управления:
 * подпись без подложки на пёстрой карте не читается вовсе.
 */
@Composable
internal fun MapLegend(legend: AnalyticsMapLegend, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(Sizes.mapNoteWidth),
        shape = RoundedCornerShape(Sizes.corner),
        tonalElevation = Sizes.dialogElevation,
        shadowElevation = Sizes.mapMarkLift
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.snug, vertical = Spacing.tight),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            SectionHeader(texts.mapLegend, legend.expanded, legend::toggle)
            Collapsible(legend.expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
                    // Порядок от спокойного к тревожному: глаз идёт сверху
                    // вниз, и так он идёт от «всё хорошо» к «надо вмешаться».
                    LegendLine(toneColor(StatusTone.Good), texts.legendGood)
                    LegendLine(toneColor(StatusTone.Idle), texts.legendIdle)
                    LegendLine(toneColor(StatusTone.Waiting), texts.legendSomeTrouble)
                    LegendLine(toneColor(StatusTone.Bad), texts.legendTrouble)
                    LegendNote(texts.legendSize)
                    LegendNote(texts.legendChosen)
                }
            }
        }
    }
}

/** Строка легенды: кружок того же цвета, каким покрашен ярлычок, и что он значит. */
@Composable
private fun LegendLine(tone: Color, words: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
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
