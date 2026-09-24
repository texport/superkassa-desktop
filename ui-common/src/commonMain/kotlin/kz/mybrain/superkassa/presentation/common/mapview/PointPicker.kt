package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress

/**
 * Окно выбора места торговой точки на карте — как его зовёт форма точки.
 *
 * Форма точки живёт в кабинете, окно карты — в своей области, и друг
 * друга они не знают: окно подставляет каркас окна, а форма отдаёт ему
 * свой адресный регистр и получает выбранное.
 */
interface PointPicker {

    /**
     * Окно карты поверх формы.
     *
     * @param registry адресный регистр: его даёт форма точки, открывшая окно.
     * @param point уже выбранное место: окно открывается на нём.
     * @param address уже выбранный адрес точки: окно открывается на нём.
     * @param onAddress адрес, выбранный в самом окне: у формы точки и у карты
     *   он один и тот же.
     */
    @Composable
    fun Show(
        registry: MapRegistry,
        point: MapPoint?,
        address: RegisterAddress?,
        onAddress: (RegisterAddress) -> Unit,
        onDismiss: () -> Unit,
        onPicked: (MapPoint) -> Unit
    )
}
