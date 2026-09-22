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
 * Сколько касс сейчас на виду и сколько из них работает по закону.
 *
 * Сеть в две тысячи касс на карте страны — это полсотни кружков с числами,
 * и сложить их глазами владелец не может: он видит, что касс много,
 * и не знает, сколько именно и всю ли сеть он сейчас видит. Счёт идёт
 * по видимому куску карты и пересчитывается при каждом её сдвиге —
 * иначе он отвечал бы не на тот вопрос, который задан глазами.
 *
 * Строка об учёте стоит второй и не прячется: в кабинете показа из 3294
 * касс на учёте четыре, и число заведённых касс само по себе говорит
 * о сети совсем не то, что о ней подумают.
 *
 * Строка об отборе появляется только при действующем отборе: без него
 * «отобрано две тысячи из двух тысяч» — шум.
 */
@Composable
internal fun MapTally(
    shown: MapCount,
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
                text = texts.mapShownOf.format(Money.count(shown.kkms), Money.count(shown.placed)),
                style = MaterialTheme.typography.titleMedium
            )
            TallyNote(texts.mapOnRecordOf.format(Money.count(shown.onRecord), Money.count(shown.kkms)))
            if (sieved) {
                TallyNote(texts.mapSievedOf.format(Money.count(shown.placed), Money.count(shown.whole)))
            }
        }
    }
}

/** Пояснительная строка итога: тише главного числа, но читается рядом с ним. */
@Composable
private fun TallyNote(words: String) {
    Text(
        text = words,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Числа итога над картой.
 *
 * Своим типом, а не четырьмя параметрами подряд: все четыре — кассы,
 * все четыре `Int`, и перепутанные местами «видно» и «поставлено»
 * не заметил бы ни разработчик, ни проверка.
 *
 * @param kkms кассы мест, попавших в окно карты.
 * @param onRecord из них стоящие на учёте КГД.
 * @param placed кассы, вставшие на карту после отбора.
 * @param whole кассы всей сети — до отбора.
 */
internal data class MapCount(val kkms: Int, val onRecord: Int, val placed: Int, val whole: Int)

/** Итог по видимым местам: считается там же, где отсеиваются ярлычки. */
internal fun mapCount(shown: List<KkmGroup>, placed: Int, whole: Int): MapCount = MapCount(
    kkms = shown.sumOf { it.size },
    onRecord = shown.sumOf(::onRecordCount),
    placed = placed,
    whole = whole
)
