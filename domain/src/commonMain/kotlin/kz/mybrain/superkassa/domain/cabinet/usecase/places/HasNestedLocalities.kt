package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.port.CabinetAddresses

/** Есть ли под пунктом вложенные пункты: у Караганды под городом лежат районы. */
class HasNestedLocalities(private val addresses: CabinetAddresses) {
    suspend operator fun invoke(localityId: Long): Boolean = addresses.nested(localityId).isNotEmpty()
}
