package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.field.SearchField
import kz.mybrain.superkassa.designsystem.field.fieldMinWidth
import kz.mybrain.superkassa.designsystem.picker.MenuChip
import kz.mybrain.superkassa.designsystem.picker.MenuSearch
import kz.mybrain.superkassa.designsystem.picker.SieveChip
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.KkmRecord
import kz.mybrain.superkassa.presentation.analytics.map.KkmMark
import kz.mybrain.superkassa.presentation.analytics.map.MapSieve
import kz.mybrain.superkassa.presentation.analytics.map.SievePlace
import kz.mybrain.superkassa.presentation.words.cabinet.recordTitle
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.fill

/**
 * Отбор касс над картой.
 *
 * Сеть в сотню касс — сотня одинаковых ярлычков на карте города, и найти
 * в ней нужную владелец мог только глазами по списку. Отбор собран так же,
 * как в картах объявлений: строка поиска, торговая точка списком и признаки
 * плашками — и карта со списком сужаются вместе.
 *
 * Плашки признаков перечислением заданы не здесь, а в самом отборе:
 * добавленный признак появляется в ряду и начинает работать без второй
 * правки.
 */
@Composable
internal fun AnalyticsSieveBar(
    sieve: MapSieve,
    places: List<SievePlace>,
    texts: AnalyticsTexts,
    onSieve: (MapSieve) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        // Плашки ниже строки поиска, и по верхнему краю они висели бы
        // над её подписью: ряд читается как один, а не как два уровня.
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        // Подпись поля короткая, а чем искать — примером внутри него:
        // длинная подпись переносилась на вторую строку, поле становилось
        // выше плашек, и ряд читался как два разных.
        SearchField(
            value = sieve.needle,
            label = texts.searchKkmLabel,
            onChange = { onSieve(sieve.copy(needle = it)) },
            // Поле и плашки делят строку: ряд отбора выходит одной ширины
            // со строкой источника над ним, а поле не торчит шире плашек.
            modifier = Modifier.weight(1f).fieldMinWidth(texts.searchKkmLabel, Sizes.fieldSearch),
            hint = texts.searchKkm,
            clearLabel = texts.sieve.clear
        )
        Marks(sieve, texts, onSieve)
        Records(sieve, texts, onSieve, Modifier.weight(1f))
        if (places.isNotEmpty()) Places(sieve, places, texts, onSieve, Modifier.weight(1f))
        if (sieve.set) {
            TextButton(onClick = { onSieve(MapSieve()) }) { Text(texts.sieve.clear) }
        }
    }
}

/** Признаки кассы плашками: нажатая оставляет на карте только такие. */
@Composable
private fun Marks(sieve: MapSieve, texts: AnalyticsTexts, onSieve: (MapSieve) -> Unit) {
    val chosen = sieve.marks
    KkmMark.entries.forEach { mark ->
        val on = mark in chosen
        SieveChip(
            selected = on,
            label = mark.title(texts.sieve),
            onClick = { onSieve(sieve.copy(marks = toggled(chosen, mark))) }
        )
    }
}

/**
 * Учёт КГД — плашкой со списком: смыслов пять, и выбирается один.
 *
 * Тем же способом, что и торговая точка ниже: нажимаемые плашки здесь
 * обещали бы, что признаки складываются, а они друг друга исключают.
 */
@Composable
private fun Records(sieve: MapSieve, texts: AnalyticsTexts, onSieve: (MapSieve) -> Unit, modifier: Modifier) {
    val chosen = sieve.record
    MenuChip(
        value = recordTitle(chosen, texts.sieve),
        options = listOf(null) + KkmRecord.entries,
        title = { recordTitle(it, texts.sieve) },
        chosen = chosen != null,
        modifier = modifier,
        onSelect = { onSieve(sieve.copy(record = it)) }
    )
}

/**
 * Торговая точка — плашкой со списком и поиском в нём.
 *
 * Точек у сети бывают тысячи: набор плашек занял бы пол-экрана, а меню
 * из двух тысяч пунктов подтормаживало при каждом открытии. Меню
 * показывает первые [PLACES_SHOWN] и ищет по названию и адресу.
 */
@Composable
private fun Places(
    sieve: MapSieve,
    places: List<SievePlace>,
    texts: AnalyticsTexts,
    onSieve: (MapSieve) -> Unit,
    modifier: Modifier
) {
    val chosen = remember(places, sieve.place) { places.firstOrNull { it.id == sieve.place } }
    val options = remember(places) { listOf(null) + places }
    MenuChip(
        value = chosen?.name ?: texts.allPlaces,
        options = options,
        title = { it?.name ?: texts.allPlaces },
        chosen = chosen != null,
        modifier = modifier,
        search = MenuSearch(texts.placeSearch, texts.placesMore.fill(PLACES_SHOWN), PLACES_SHOWN),
        onSelect = { onSieve(sieve.copy(place = it?.id)) }
    )
}

/** Сколько точек меню показывает разом; остальные находятся поиском. */
private const val PLACES_SHOWN = 50

/** Нажатая плашка добавляется к отбору, нажатая повторно — убирается. */
private fun toggled(marks: Set<KkmMark>, mark: KkmMark): Set<KkmMark> =
    if (mark in marks) marks - mark else marks + mark
