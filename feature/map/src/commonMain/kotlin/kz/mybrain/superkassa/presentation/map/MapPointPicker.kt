package kz.mybrain.superkassa.presentation.map

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.presentation.common.mapview.MapPoint
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.common.mapview.MapRegistry
import kz.mybrain.superkassa.presentation.common.mapview.PointPicker

/**
 * Выбор места торговой точки окном карты — на службах карты окна.
 *
 * Каркас окна отдаёт его форме точки: форма знает только [PointPicker],
 * а службы карты и само окно остаются здесь.
 */
class MapPointPicker(private val services: MapPorts) : PointPicker {

    @Composable
    override fun Show(
        registry: MapRegistry,
        point: MapPoint?,
        address: RegisterAddress?,
        onAddress: (RegisterAddress) -> Unit,
        onDismiss: () -> Unit,
        onPicked: (MapPoint) -> Unit
    ) = MapPickerDialog(services, registry, point, address, onAddress, onDismiss, onPicked)
}
