package kz.mybrain.superkassa.domain.cabinet.usecase.card

import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCards

/** Регистрационная карта той или иной версии: что в ней было записано. */
class ReadCardVersion(private val cards: CabinetCards) {
    suspend operator fun invoke(registerId: String, version: Int): RegistrationCard = cards.version(registerId, version)
}
