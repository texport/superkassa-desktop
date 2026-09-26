package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.analytics.map.KkmGroup
import kz.mybrain.superkassa.presentation.analytics.map.onRecordCount
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.fill

/**
 * Сколько касс сейчас на виду и сколько из них работает по закону —
 * первой строкой раскрытой легенды.
 *
 * Сеть в две тысячи касс на карте страны — это полсотни кружков с числами,
 * и сложить их глазами владелец не может. Счёт идёт по видимому куску
 * карты и пересчитывается при каждом её сдвиге.
 *
 * Прежде счёт стоял отдельной плашкой поверх карты и закрывал её угол,
 * а в узком окне растягивался на всю ширину и закрывал карту целиком.
 * Теперь он — строка легенды: там же, где объяснено, что на карте.
 *
 * Об отборе сказано только при действующем отборе: без него «отобрано
 * две тысячи из двух тысяч» — шум.
 */
@Composable
internal fun TallyLine(shown: MapCount?, sieved: Boolean, texts: AnalyticsTexts) {
    shown ?: return
    val parts = listOfNotNull(
        "${texts.mapShown}: ${texts.mapShownOf.fill(Money.count(shown.kkms), Money.count(shown.placed))}",
        texts.mapOnRecordOf.fill(Money.count(shown.onRecord), Money.count(shown.kkms)).lowercase(),
        texts.mapSievedOf.fill(Money.count(shown.placed), Money.count(shown.whole)).lowercase().takeIf { sieved }
    )
    Text(
        text = parts.joinToString(Glyphs.SEPARATOR),
        style = MaterialTheme.typography.bodySmall,
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
