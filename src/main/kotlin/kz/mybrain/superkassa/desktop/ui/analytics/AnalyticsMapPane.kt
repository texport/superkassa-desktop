package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.map.MapControls
import kz.mybrain.superkassa.desktop.ui.map.MapMarks
import kz.mybrain.superkassa.desktop.ui.map.MapServices
import kz.mybrain.superkassa.desktop.ui.map.MapView
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Все кассы компании на карте.
 *
 * Слева карта с ярлычками мест и карточкой выбранной кассы под ней,
 * справа — все кассы списком. Оба живут от одного ответа кабинета:
 * касса не исчезает из раздела оттого, что её негде поставить.
 *
 * Кассы одного места сведены в один ярлычок с числом — иначе в торговой
 * точке с тремя кассами булавки садились одна на другую. Путь владельца
 * тот же, что и в картах объявлений: отбор сверху, ярлычок с числом
 * на карте, список касс места под ней, и из него — в аналитику одной
 * кассы.
 *
 * При источнике «адрес торговой точки» координат кабинет не даёт вовсе,
 * и точки появляются постепенно — по мере того, как карта находит дома.
 * Поэтому карта ведётся к кассам один раз за загрузку: иначе она
 * возвращалась бы в середину набора после каждого найденного адреса.
 */
@Composable
fun AnalyticsMapPane(
    session: Session,
    cabinet: CabinetSession,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts
) {
    val services = remember(session.preferences) { MapServices(session.preferences) }
    val model = remember(cabinet) { AnalyticsMapModel(cabinet, services.geocoder) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(cabinet.token, model.source) { model.load() }
    LaunchedEffect(model.view) { model.findAddresses() }
    // Карта ведётся к кассам по всему набору, а не по отобранному:
    // иначе она прыгала бы к остатку при каждой нажатой плашке.
    val whole = placement(model.view, model.points)
    LaunchedEffect(whole.placed.size) { model.centre(whole.placed) }
    val placement = sieved(whole, model.sieve)
    val groups = kkmGroups(placement.placed, model.map.zoom)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        AnalyticsSourceBar(model, placement, texts) { scope.launch { model.load() } }
        AnalyticsSieveBar(model, sievePlaces(model.view), texts)
        val trouble = model.trouble
        val state = when {
            trouble != null -> analyticsTroubleState(trouble, texts) { scope.launch { model.load() } }
            model.view == null -> ScreenState.Working
            else -> ScreenState.Ready
        }
        ScreenSlot(state, Modifier.weight(1f)) {
            MapBody(session, model, services, placement, groups, texts, cabinetTexts, Modifier.weight(1f))
        }
    }
    // Окно аналитики кассы живёт поверх карты: закрыв его, владелец
    // возвращается к тому же месту и тому же отбору.
    model.opened?.let { kkm ->
        AnalyticsKkmDialog(session, cabinet, kkm, texts, cabinetTexts) { model.opened = null }
    }
}

/** Карта с точками, карточка выбранной кассы и список непоставленных. */
@Composable
private fun MapBody(
    session: Session,
    model: AnalyticsMapModel,
    services: MapServices,
    placement: Placement,
    groups: List<KkmGroup>,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.normal)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            MapWindow(session, model, services, placement, groups, texts, cabinetTexts, Modifier.weight(1f))
            UnderMap(model, placement, groups, texts, cabinetTexts)
        }
        // Список всех касс, а не только непоставленных: точки на карте
        // неотличимы, и владелец сети искал свою кассу глазами.
        AnalyticsKkmList(
            placed = placement.placed,
            unplaced = placement.unplaced,
            chosen = model.chosen,
            source = model.source,
            texts = texts,
            onChoose = { row -> model.show(row, groups) },
            modifier = Modifier.width(Sizes.unplacedColumn).fillMaxHeight()
        )
    }
}

/**
 * Само окно карты.
 *
 * Пока ни одной точки нет, карта заменяется объяснением: пустая карта
 * города говорит владельцу не больше, чем пустой экран.
 */
@Composable
private fun MapWindow(
    session: Session,
    model: AnalyticsMapModel,
    services: MapServices,
    placement: Placement,
    groups: List<KkmGroup>,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    modifier: Modifier = Modifier
) {
    if (placement.placed.isEmpty()) {
        EmptyState(
            icon = AppIcons.place,
            title = if (model.sieve.set) texts.sieveEmpty else texts.mapEmpty,
            hint = if (model.sieve.set) texts.sieveEmptyHint else texts.mapEmptyHint,
            modifier = modifier,
            centered = true
        )
        return
    }
    Box(modifier = modifier) {
        MapView(
            state = model.map,
            tiles = services.tiles,
            modifier = Modifier.fillMaxSize(),
            // Нажатие мимо ярлычка снимает выбор: раскрытое место
            // закрывается тем же способом, каким открылось.
            onTap = { _, _ -> model.forget() },
            overlay = { canvas ->
                MapMarks(model.map, canvas, groups.map { it.mark(model) }) { mark ->
                    model.open(groups.first { it.id == mark.id })
                }
            }
        )
        MapControls(
            state = model.map,
            texts = cabinetTexts,
            preferences = session.preferences,
            modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.snug)
        )
    }
}
