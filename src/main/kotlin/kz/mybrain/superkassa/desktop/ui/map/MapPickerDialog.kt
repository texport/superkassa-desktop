package kz.mybrain.superkassa.desktop.ui.map

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
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.mapAddressTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

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
 * @param address уже выбранный адрес точки: окно открывается на нём.
 * @param onAddress адрес, выбранный в самом окне: у формы точки и у карты
 *   он один и тот же.
 */
@Composable
fun MapPickerDialog(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    latitude: BigDecimal?,
    longitude: BigDecimal?,
    address: RegisterAddress?,
    onAddress: (RegisterAddress) -> Unit,
    onDismiss: () -> Unit,
    onPicked: (BigDecimal, BigDecimal) -> Unit
) {
    // Набор живёт, пока открыто окно: метка, поставленная владельцем,
    // не должна пропадать ни от одной перерисовки.
    val parts = remember {
        MapPickerParts(
            services = MapServices(session.preferences),
            pick = MapAddressPick(address, session.language),
            state = MapState().also {
                if (latitude != null && longitude != null) {
                    it.show(latitude.toDouble(), longitude.toDouble(), HOUSE_ZOOM)
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            // Высота окна задана, а карта берёт остаток: при высоте
            // по содержимому карта распирала окно, и подсказку с шапкой
            // выдавливало за верхний край.
            modifier = Modifier.width(Sizes.mapWidth).height(Sizes.mapDialogHeight),
            shape = RoundedCornerShape(Sizes.corner),
            tonalElevation = Sizes.dialogElevation
        ) {
            PickerBody(session, cabinet, texts, parts, address, onAddress, onDismiss, onPicked)
        }
    }
}

/**
 * Из чего собрано окно: службы карты, состояние карты и выбранный в окне адрес.
 *
 * Собраны вместе потому, что живут одну жизнь с окном и нужны всем
 * его рядам: по отдельности они протягивались бы семью параметрами.
 */
private class MapPickerParts(
    val services: MapServices,
    val pick: MapAddressPick,
    val state: MapState
)

/**
 * Ряды окна сверху вниз: шаги регистра, карта, подбор по метке,
 * градусы руками и подвал с выбранным.
 */
@Composable
private fun PickerBody(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    parts: MapPickerParts,
    address: RegisterAddress?,
    onAddress: (RegisterAddress) -> Unit,
    onDismiss: () -> Unit,
    onPicked: (BigDecimal, BigDecimal) -> Unit
) {
    val state = parts.state
    val notices = remember(session.language) { mapAddressTexts(session.language) }
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.normal),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        MapHeader(texts, onDismiss)
        RegistryAddress(
            session, cabinet, texts, state, parts.services.geocoder,
            address, parts.pick, notices, onAddress
        )
        // Карта видна и до выбора адреса: когда адрес подбирается по метке,
        // метка ставится раньше него. Остаток высоты — карте: чем меньше
        // шагов раскрыто, тем больше видно улицы вокруг метки.
        MapArea(state, parts.services.tiles, texts, session.preferences, Modifier.weight(1f))
        PointAddress(cabinet, state, parts.services.reverse, notices) { chosen ->
            parts.pick.byPoint(chosen, session.language)
            onAddress(chosen)
        }
        DegreesEntry(state, texts)
        MapFooter(state, texts, onDismiss, onPicked)
    }
}

/** Увеличение, на котором виден город: с него начинается найденное по адресу подключения. */
internal const val CITY_ZOOM = 12

/** Увеличение, на котором различимы дома: на нём открывается найденный адрес. */
internal const val HOUSE_ZOOM = 17
