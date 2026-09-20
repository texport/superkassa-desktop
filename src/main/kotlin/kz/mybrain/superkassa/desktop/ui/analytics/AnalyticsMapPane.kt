package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import kz.mybrain.superkassa.desktop.ui.map.MapPin
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
 * Слева карта с точками и карточкой выбранной кассы под ней, справа —
 * кассы, которых на карте нет. Оба списка живут от одного ответа
 * кабинета: касса не исчезает из раздела оттого, что её негде поставить.
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
    val placement = placement(model.view, model.points)
    LaunchedEffect(placement.placed.size) { model.centre(placement.placed) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        AnalyticsSourceBar(model, placement, texts) { scope.launch { model.load() } }
        val trouble = model.trouble
        val state = when {
            trouble != null -> analyticsTroubleState(trouble, texts) { scope.launch { model.load() } }
            model.view == null -> ScreenState.Working
            else -> ScreenState.Ready
        }
        ScreenSlot(state, Modifier.weight(1f)) {
            MapBody(session, model, services, placement, texts, cabinetTexts, Modifier.weight(1f))
        }
    }
}

/** Карта с точками, карточка выбранной кассы и список непоставленных. */
@Composable
private fun MapBody(
    session: Session,
    model: AnalyticsMapModel,
    services: MapServices,
    placement: Placement,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.normal)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            MapWindow(session, model, services, placement, texts, cabinetTexts, Modifier.weight(1f))
            AnalyticsPinCard(
                kkm = placement.placed.firstOrNull { it.kkm.cashRegisterId == model.chosen }?.kkm,
                source = model.source,
                texts = texts,
                cabinet = cabinetTexts,
                modifier = Modifier.height(Sizes.pinCardHeight)
            )
        }
        AnalyticsUnplaced(
            rows = placement.unplaced,
            source = model.source,
            texts = texts,
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
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    modifier: Modifier = Modifier
) {
    if (placement.placed.isEmpty()) {
        EmptyState(
            icon = AppIcons.place,
            title = texts.mapEmpty,
            hint = texts.mapEmptyHint,
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
            pins = placement.placed.map { it.pin(model.chosen) },
            onPin = { pin -> model.chosen = pin?.id }
        )
        MapControls(
            state = model.map,
            texts = cabinetTexts,
            preferences = session.preferences,
            modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.snug)
        )
    }
}

/** Касса — знаком на карте; выбранная рисуется крупнее и главным цветом. */
private fun PlacedKkm.pin(chosen: String?): MapPin =
    MapPin(kkm.cashRegisterId, latitude, longitude, kkm.cashRegisterId == chosen)
