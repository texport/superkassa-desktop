package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAddresses

/** Адрес, подтверждённый регистром по коду РКА выбранного дома. */
class ResolveAddress(private val addresses: CabinetAddresses) {
    suspend operator fun invoke(rka: String): RegisterAddress = addresses.resolve(rka)
}
