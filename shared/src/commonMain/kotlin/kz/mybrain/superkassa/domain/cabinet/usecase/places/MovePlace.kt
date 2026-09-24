package kz.mybrain.superkassa.domain.cabinet.usecase.places

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.ChangeAddressResult
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceAddress
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces

/**
 * Переезд точки. Прямо адрес меняется только у точки без касс или с одними
 * черновиками; иначе кабинет называет кассы, которым нужна перерегистрация.
 *
 * Называет он их только идентификатором и состоянием, а владельцу нужны
 * название, номер КГД или заводской номер: их берём у тех же касс среди
 * [known]. Прежде строка «сначала перерегистрируйте кассы» не называла
 * ни одной.
 */
class MovePlace(private val places: CabinetPlaces) {
    suspend operator fun invoke(
        place: RetailPlace,
        address: RegisterAddress,
        latitude: Decimal,
        longitude: Decimal,
        known: List<CabinetRegister>
    ): ChangeAddressResult {
        val result = places.move(place.id, RetailPlaceAddress(address.addressRef, latitude, longitude))
        return result.copy(blockingCashRegisters = result.blockingCashRegisters.map { it.namedBy(known) })
    }
}
