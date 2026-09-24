package kz.mybrain.superkassa.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.presentation.common.mapview.HOUSE_ZOOM
import kz.mybrain.superkassa.presentation.common.mapview.MapCases
import kz.mybrain.superkassa.presentation.common.mapview.MapLocating
import kz.mybrain.superkassa.presentation.common.mapview.MapPoint
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.common.mapview.MapRegistry
import kz.mybrain.superkassa.presentation.common.mapview.MapState
import kz.mybrain.superkassa.presentation.common.mapview.MapTiles
import kz.mybrain.superkassa.presentation.common.mapview.degrees
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Выбор места торговой точки на карте.
 *
 * Собрано на `Dialog` с собственной поверхностью, а не на `AlertDialog`:
 * у того содержимое живёт в прокручиваемой средней части с рассчитанной
 * высотой, и карта в четыреста точек её распирала — заголовок с подсказкой
 * уходили за верхний край окна, а строка координат наезжала на карту.
 *
 * Адрес окно не придумывает: он выбирается в государственном регистре
 * и уходит наружу тем же, каким его подтвердил регистр. Путей к нему два,
 * и оба кончаются записью регистра: шаги регистра сверху (см.
 * [RegistryAddress]) — и тогда карта идёт к найденному дому; подбор
 * по метке снизу (см. [PointAddress]) — и тогда место называет служба
 * карт, а запись всё равно подтверждает регистр. Координаты в обоих
 * случаях даёт карта: в регистре их нет.
 *
 * @param services службы карты: плитки, поиск места, определение своего места.
 * @param registry адресный регистр: его даёт форма точки, открывшая окно.
 * @param point уже выбранное место: окно открывается на нём.
 * @param address уже выбранный адрес точки: окно открывается на нём.
 * @param onAddress адрес, выбранный в самом окне: у формы точки и у карты
 *   он один и тот же.
 */
@Composable
internal fun MapPickerDialog(
    services: MapPorts,
    registry: MapRegistry,
    point: MapPoint?,
    address: RegisterAddress?,
    onAddress: (RegisterAddress) -> Unit,
    onDismiss: () -> Unit,
    onPicked: (MapPoint) -> Unit
) {
    val language = LocalLanguage.current
    val texts = textsOf(language).cabinet
    val parts = rememberPickerParts(services, registry, address, language, point)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            // Высота окна задана, а карта берёт остаток: при высоте
            // по содержимому карта распирала окно, и подсказку с шапкой
            // выдавливало за верхний край.
            modifier = Modifier.width(Sizes.mapWidth).height(Sizes.mapDialogHeight),
            shape = RoundedCornerShape(Sizes.corner),
            tonalElevation = Sizes.dialogElevation
        ) {
            MapPickerBody(texts, parts, address, onAddress, onDismiss, onPicked)
        }
    }
}

/**
 * Набор окна живёт, пока окно открыто: метка, поставленная владельцем,
 * не должна пропадать ни от одной перерисовки.
 */
@Composable
private fun rememberPickerParts(
    services: MapPorts,
    registry: MapRegistry,
    address: RegisterAddress?,
    language: Language,
    point: MapPoint?
): MapPickerParts {
    return remember {
        val state = MapState()
        if (point != null) state.show(point.latitude.degrees(), point.longitude.degrees(), HOUSE_ZOOM)
        val cases = services.cases()
        MapPickerParts(
            cases = cases,
            tiles = cases.tiles(),
            locating = cases.locating(),
            registry = registry,
            pick = MapAddressPick(address, language),
            state = state
        )
    }
}

/**
 * Из чего собрано окно: службы карты, плитки, определение места,
 * адресный регистр, состояние карты и выбранный в окне адрес.
 *
 * Собраны вместе потому, что живут одну жизнь с окном и нужны всем
 * его рядам: по отдельности они протягивались бы семью параметрами.
 */
class MapPickerParts(
    val cases: MapCases,
    val tiles: MapTiles,
    val locating: MapLocating,
    val registry: MapRegistry,
    val pick: MapAddressPick,
    val state: MapState
)

/**
 * Ряды окна сверху вниз: шаги регистра, карта, подбор по метке,
 * градусы руками и подвал с выбранным.
 *
 * Окно ставит их на свою поверхность; снимки окна у каркаса ставят их
 * на свою — с заданным состоянием карты и регистром кабинета.
 */
@Composable
fun MapPickerBody(
    texts: CabinetTexts,
    parts: MapPickerParts,
    address: RegisterAddress?,
    onAddress: (RegisterAddress) -> Unit,
    onDismiss: () -> Unit,
    onPicked: (MapPoint) -> Unit
) {
    val language = LocalLanguage.current
    val state = parts.state
    val notices = remember(language) { textsOf(language).map.address }
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        MapHeader(texts, onDismiss)
        RegistryAddress(parts, address, onAddress)
        // Карта видна и до выбора адреса: когда адрес подбирается по метке,
        // метка ставится раньше него. Остаток высоты — карте: чем меньше
        // шагов раскрыто, тем больше видно улицы вокруг метки.
        MapArea(state, parts.tiles, texts, parts.locating, Modifier.weight(1f))
        PointAddress(parts.registry, state, parts.cases.namePoint, notices) { chosen ->
            parts.pick.byPoint(chosen, language)
            onAddress(chosen)
        }
        DegreesEntry(state, texts)
        MapFooter(state, texts, onDismiss, onPicked)
    }
}
