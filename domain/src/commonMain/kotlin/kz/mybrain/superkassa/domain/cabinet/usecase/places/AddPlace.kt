package kz.mybrain.superkassa.domain.cabinet.usecase.places

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.cabinet.model.PlaceAdded
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceCreate
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces

/**
 * Заводит торговую точку: название, адрес из регистра и место на карте.
 * Название уходит без пробелов по краям: владелец их не видит.
 *
 * Точку с тем же адресом и тем же местом кабинет второй раз не заводит:
 * он молча отдаёт уже заведённую, с её прежним названием. Такой ответ
 * узнаётся по точке, которая уже есть среди [known], — владельцу надо
 * сказать, что новой точки нет, а не закрыть окно, как будто она есть.
 */
class AddPlace(private val places: CabinetPlaces) {
    suspend operator fun invoke(
        name: String,
        address: RegisterAddress,
        latitude: Decimal,
        longitude: Decimal,
        known: List<RetailPlace>
    ): PlaceAdded {
        val place = places.add(RetailPlaceCreate(name.trim(), address.addressRef, latitude, longitude))
        return PlaceAdded(place, existed = known.any { it.id == place.id })
    }
}
