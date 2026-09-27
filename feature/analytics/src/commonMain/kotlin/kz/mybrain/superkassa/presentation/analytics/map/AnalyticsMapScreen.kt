package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.analytics.model.Placement
import kz.mybrain.superkassa.domain.analytics.model.placement
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts
import kz.mybrain.superkassa.presentation.analytics.common.analyticsScreenState
import kz.mybrain.superkassa.presentation.analytics.kkm.AnalyticsKkmDialog
import kz.mybrain.superkassa.presentation.analytics.map.component.AnalyticsSieveBar
import kz.mybrain.superkassa.presentation.analytics.map.component.AnalyticsSourceBar
import kz.mybrain.superkassa.presentation.analytics.map.component.UnderMap
import kz.mybrain.superkassa.presentation.common.mapview.MapFold
import kz.mybrain.superkassa.presentation.common.mapview.MapLocating
import kz.mybrain.superkassa.presentation.common.mapview.MapTiles
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Все кассы компании на карте.
 *
 * Слева карта с ярлычками мест, справа — все кассы списком и карточка
 * выбранной под ним; в узком окне список встаёт под карту. Оба живут
 * от одного ответа кабинета: касса не исчезает из раздела оттого, что её
 * негде поставить.
 *
 * Кассы одного места сведены в один ярлычок с числом — иначе в торговой
 * точке с тремя кассами булавки садились одна на другую. Путь владельца
 * тот же, что и в картах объявлений: отбор сверху, ярлычок с числом
 * на карте, список касс места рядом, и из него — в аналитику одной
 * кассы.
 *
 * При источнике «адрес торговой точки» координат кабинет не даёт вовсе,
 * и точки появляются постепенно — по мере того, как карта находит дома.
 * Поэтому карта ведётся к кассам один раз за загрузку: иначе она
 * возвращалась бы в середину набора после каждого найденного адреса.
 */
@Composable
internal fun AnalyticsMapScreen(
    ports: AnalyticsPorts,
    model: AnalyticsMapViewModel,
    tools: MapTools,
    access: String?,
    words: MapWords
) {
    val texts = words.texts
    val state by model.state.collectAsScreenState()
    LaunchedEffect(access) { model.follow(access) }
    val parts = mapParts(state, model, tools, words)

    HeadOverMap(
        modifier = Modifier.fillMaxSize(),
        head = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
                AnalyticsSourceBar(state, parts.placement, texts, model)
                // Точки отбора собираются по всем кассам: пересобирать их
                // на каждый сдвиг карты и каждый найденный адрес незачем.
                val reading = state.reading.value
                val places = remember(reading) { sievePlaces(reading) }
                AnalyticsSieveBar(state.sieve, places, texts, model::sift)
            }
        }
    ) {
        ScreenSlot(analyticsScreenState(state.reading, texts, model::refresh) { null }, Modifier.fillMaxSize()) {
            MapBody(parts, Modifier.fillMaxSize())
        }
    }
    // Окно аналитики кассы живёт поверх карты: закрыв его, владелец
    // возвращается к тому же месту и тому же отбору.
    state.opened?.let { kkm -> AnalyticsKkmDialog(ports, kkm, access, words) { model.openSales(null) } }
}

/**
 * Расстановка касс по ответу и найденным адресам.
 *
 * Карта ведётся к кассам по всему набору, а отбор сужает уже поставленное:
 * иначе она прыгала бы к остатку при каждой нажатой плашке. Места
 * пересобираются только при смене набора или увеличения, а не на каждом
 * кадре: у сети в две тысячи касс раскладка по клеткам повторялась бы
 * при каждом сдвиге карты, ничего не меняя.
 */
@Composable
internal fun mapParts(
    state: AnalyticsMapUiState,
    actions: AnalyticsMapActions,
    tools: MapTools,
    words: MapWords
): MapParts {
    val whole = remember(state.reading.value, state.found) { placement(state.reading.value, state) }
    val placement = remember(whole, state.sieve) { sieved(whole, state.sieve) }
    val cell = groupCell(LocalDensity.current.density)
    val groups = remember(placement.placed, state.map.zoom, cell) { kkmGroups(placement.placed, state.map.zoom, cell) }
    return MapParts(state, actions, tools, MapLaid(placement, whole.placed.size, groups), words)
}

/**
 * Чем карта касс рисуется: плитки, определение своего места и то,
 * как владелец разложил карточку, легенду и список касс. Живут, пока открыто окно.
 */
internal class MapTools(
    val tiles: MapTiles,
    val locating: MapLocating,
    val panel: MapFold,
    val legend: MapFold,
    val list: MapFold
)

/**
 * Где кассы встали на карте после отбора.
 *
 * @param whole сколько касс встало на карту до отбора: с этим числом
 *   сверяется итог в углу.
 */
internal class MapLaid(val placement: Placement, val whole: Int, val groups: List<KkmGroup>)

/** Слова раздела карты: свои и кабинета, из которого взяты подписи карты. */
internal class MapWords(val texts: AnalyticsTexts, val cabinetTexts: CabinetTexts)

/**
 * Из чего собран раздел карты.
 *
 * Состояние, действия, средства карты, расстановка и надписи нужны каждому
 * ряду раздела и окну во весь экран; по отдельности они протягивались бы
 * восемью параметрами через три вызова.
 */
internal class MapParts(
    val state: AnalyticsMapUiState,
    val actions: AnalyticsMapActions,
    val tools: MapTools,
    val laid: MapLaid,
    val words: MapWords
) {
    val placement: Placement get() = laid.placement
    val whole: Int get() = laid.whole
    val groups: List<KkmGroup> get() = laid.groups
    val texts: AnalyticsTexts get() = words.texts
    val cabinetTexts: CabinetTexts get() = words.cabinetTexts
}

/**
 * Карта с точками, а рядом список касс и карточка выбранной.
 *
 * Раскрытая во всё окно карта заменяет раздел, а не ложится поверх него:
 * две карты одного состояния тянули бы плитки на два окна разного размера.
 * Возврат из окна собирает раздел заново на том же состоянии — выбор
 * и место карты остаются.
 */
@Composable
internal fun MapBody(parts: MapParts, modifier: Modifier = Modifier) {
    var fullscreen by remember { mutableStateOf(false) }
    if (fullscreen) {
        AnalyticsMapFullscreen(parts) { fullscreen = false }
        return
    }
    MapAndDetails(
        modifier = modifier,
        map = { MapWindow(parts, fullscreen = false, onFullscreen = { fullscreen = true }, Modifier.fillMaxSize()) },
        list = { KkmList(parts) },
        card = { UnderMap(parts) },
        listOpen = parts.tools.list.expanded
    )
}
