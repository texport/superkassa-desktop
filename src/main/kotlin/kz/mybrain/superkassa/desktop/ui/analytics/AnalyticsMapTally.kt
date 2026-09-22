package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Сколько касс сейчас на виду.
 *
 * Сеть в две тысячи касс на карте страны — это полсотни кружков с числами,
 * и сложить их глазами владелец не может: он видит, что касс много,
 * и не знает, сколько именно и всю ли сеть он сейчас видит. Счёт идёт
 * по видимому куску карты и пересчитывается при каждом её сдвиге —
 * иначе он отвечал бы не на тот вопрос, который задан глазами.
 *
 * Вторая строка появляется только при действующем отборе: без него
 * «отобрано две тысячи из двух тысяч» — шум.
 *
 * @param shown кассы мест, попавших в окно карты.
 * @param placed кассы, вставшие на карту после отбора.
 * @param whole кассы всей сети — до отбора.
 */
@Composable
internal fun MapTally(
    shown: Int,
    placed: Int,
    whole: Int,
    sieved: Boolean,
    texts: AnalyticsTexts,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Sizes.corner),
        tonalElevation = Sizes.dialogElevation,
        shadowElevation = Sizes.mapMarkLift
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.snug, vertical = Spacing.tight),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            Text(text = texts.mapShown, style = MaterialTheme.typography.labelMedium)
            Text(
                text = texts.mapShownOf.format(Money.count(shown), Money.count(placed)),
                style = MaterialTheme.typography.titleMedium
            )
            if (sieved) {
                Text(
                    text = texts.mapSievedOf.format(Money.count(placed), Money.count(whole)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
