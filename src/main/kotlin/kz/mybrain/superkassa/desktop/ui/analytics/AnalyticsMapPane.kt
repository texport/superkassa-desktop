package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.map.MapServices
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
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
    val panel = remember(session.preferences) { AnalyticsMapCard(session.preferences) }
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
            val parts = MapParts(session, model, services, placement, groups, texts, cabinetTexts, panel)
            MapBody(parts, Modifier.weight(1f))
        }
    }
    // Окно аналитики кассы живёт поверх карты: закрыв его, владелец
    // возвращается к тому же месту и тому же отбору.
    model.opened?.let { kkm ->
        AnalyticsKkmDialog(session, cabinet, kkm, texts, cabinetTexts) { model.opened = null }
    }
}

/**
 * Из чего собран раздел карты.
 *
 * Сеанс, состояние, службы карты, расстановка и надписи нужны каждому
 * ряду раздела и окну во весь экран; по отдельности они протягивались бы
 * восемью параметрами через три вызова.
 */
internal class MapParts(
    val session: Session,
    val model: AnalyticsMapModel,
    val services: MapServices,
    val placement: Placement,
    val groups: List<KkmGroup>,
    val texts: AnalyticsTexts,
    val cabinetTexts: CabinetTexts,
    val panel: AnalyticsMapCard
)

/**
 * Карта с точками, карточка выбранной кассы и список касс рядом.
 *
 * Раскрытая во всё окно карта заменяет раздел, а не ложится поверх него:
 * две карты одного состояния тянули бы плитки на два окна разного размера.
 * Возврат из окна собирает раздел заново на том же состоянии — выбор
 * и место карты остаются.
 */
@Composable
private fun MapBody(parts: MapParts, modifier: Modifier = Modifier) {
    var fullscreen by remember { mutableStateOf(false) }
    if (fullscreen) {
        AnalyticsMapFullscreen(parts) { fullscreen = false }
        return
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.normal)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            MapWindow(parts, fullscreen = false, onFullscreen = { fullscreen = true }, modifier = Modifier.weight(1f))
            UnderMap(parts.model, parts.placement, parts.groups, parts.texts, parts.cabinetTexts, parts.panel)
        }
        // Список всех касс, а не только непоставленных: точки на карте
        // неотличимы, и владелец сети искал свою кассу глазами.
        AnalyticsKkmList(
            placed = parts.placement.placed,
            unplaced = parts.placement.unplaced,
            chosen = parts.model.chosen,
            source = parts.model.source,
            texts = parts.texts,
            onChoose = { row -> parts.model.show(row, parts.groups) },
            modifier = Modifier.width(Sizes.unplacedColumn).fillMaxHeight(),
            sieved = parts.model.sieve.set
        )
    }
}
