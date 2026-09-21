package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.MenuChip
import kz.mybrain.superkassa.desktop.ui.components.SearchField
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
fun AnalyticsSieveBar(model: AnalyticsMapModel, places: List<SievePlace>, texts: AnalyticsTexts) {
    val sieve = model.sieve
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        // Плашки ниже строки поиска, и по верхнему краю они висели бы
        // над её подписью: ряд читается как один, а не как два уровня.
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        SearchField(
            value = sieve.needle,
            label = texts.searchKkm,
            onChange = { model.sieve = sieve.copy(needle = it) },
            modifier = Modifier.fieldWidth(texts.searchKkm, Sizes.fieldSearch),
            clearLabel = texts.sieveClear
        )
        Marks(model, texts)
        if (places.isNotEmpty()) Places(model, places, texts)
        if (sieve.set) {
            TextButton(onClick = { model.sieve = MapSieve() }) { Text(texts.sieveClear) }
        }
    }
}

/** Признаки кассы плашками: нажатая оставляет на карте только такие. */
@Composable
private fun Marks(model: AnalyticsMapModel, texts: AnalyticsTexts) {
    val chosen = model.sieve.marks
    KkmMark.entries.forEach { mark ->
        FilterChip(
            selected = mark in chosen,
            onClick = { model.sieve = model.sieve.copy(marks = toggled(chosen, mark)) },
            label = { Text(mark.title(texts)) }
        )
    }
}

/**
 * Торговая точка — плашкой со списком.
 *
 * Точек у сети десятки, и набор плашек занял бы пол-экрана: выбор
 * из списка здесь тот же, что и у отбора по смене в журнале кассы.
 */
@Composable
private fun Places(model: AnalyticsMapModel, places: List<SievePlace>, texts: AnalyticsTexts) {
    val chosen = places.firstOrNull { it.id == model.sieve.place }
    MenuChip(
        value = chosen?.name ?: texts.allPlaces,
        options = listOf(null) + places,
        title = { it?.name ?: texts.allPlaces },
        chosen = chosen != null,
        onSelect = { model.sieve = model.sieve.copy(place = it?.id) }
    )
}

/** Нажатая плашка добавляется к отбору, нажатая повторно — убирается. */
private fun toggled(marks: Set<KkmMark>, mark: KkmMark): Set<KkmMark> =
    if (mark in marks) marks - mark else marks + mark
