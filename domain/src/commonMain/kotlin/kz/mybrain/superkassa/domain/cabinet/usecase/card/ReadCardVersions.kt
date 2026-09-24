package kz.mybrain.superkassa.domain.cabinet.usecase.card

import kz.mybrain.superkassa.domain.cabinet.model.RegistrationCardVersion
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCards

/** Версии регистрационной карты: новые сверху — последняя перерегистрация нужнее давней. */
class ReadCardVersions(private val cards: CabinetCards) {
    suspend operator fun invoke(registerId: String): List<RegistrationCardVersion> =
        cards.versions(registerId).sortedByDescending { it.version }
}
