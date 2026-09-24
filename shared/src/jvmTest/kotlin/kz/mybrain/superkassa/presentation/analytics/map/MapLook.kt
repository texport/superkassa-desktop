package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.analytics.model.AddressAnswer
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.Placement
import kz.mybrain.superkassa.domain.analytics.model.placement
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.analytics.map.component.AnalyticsSieveBar
import kz.mybrain.superkassa.presentation.analytics.map.component.AnalyticsSourceBar
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Разметка раздела карты так, как её собирает `AnalyticsMapScreen`.
 *
 * Сам раздел спрашивает кабинет через модель, а снимку нужно подставить
 * «сто касс» или «ни одной» снаружи. Здесь те же ряды и то же тело
 * раздела на состоянии, собранном руками: на снимке видно ровно то,
 * что увидит владелец.
 */
@Composable
internal fun MapLook(
    model: AnalyticsMapUiState,
    laid: Placement,
    groups: List<KkmGroup>,
    view: KkmMapView?,
    whole: Int = laid.placed.size
) {
    val actions = object : AnalyticsMapActions {}
    val tools = remember { AnalyticsLook.tools() }
    val parts = MapParts(model, actions, tools, MapLaid(laid, whole, groups), AnalyticsLook.words)
    val head = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
            AnalyticsSourceBar(model, laid, AnalyticsLook.texts, actions)
            AnalyticsSieveBar(model.sieve, sievePlaces(view), AnalyticsLook.texts) {}
        }
    }
    HeadOverMap(Modifier.fillMaxSize(), head = head) { MapBody(parts, Modifier.fillMaxSize()) }
}

/**
 * Кассы, разложенные по карте.
 *
 * Координаты приходят не от кабинета, а от поиска по адресу, и здесь его
 * заменяет заданный ответ: `null` означает «ещё ищем», отсутствие адреса
 * в наборе — «не нашли». Так на снимке получаются все три причины,
 * по которым кассы нет на карте.
 */
internal fun laidOut(view: KkmMapView, found: Map<String, Pair<Double, Double>?>): Placement =
    placement(view) { address ->
        if (address !in found) return@placement AddressAnswer.Missing
        val point = found[address] ?: return@placement AddressAnswer.Searching
        AddressAnswer.Found(point.first, point.second)
    }

/**
 * Ярлычки мест по разложенным кассам.
 *
 * Увеличение берётся у самой карты, как и в разделе: клетка места
 * считается в точках полотна, и на увеличении страны в один ярлычок
 * сходится то, что на увеличении города стоит порознь.
 */
internal fun groupsOf(laid: Placement, zoom: Int): List<KkmGroup> = kkmGroups(laid.placed, zoom)
