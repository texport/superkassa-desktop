package kz.mybrain.superkassa.domain.cabinet.usecase.card

import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCards

/** Действующая регистрационная карта кассы. */
class ReadRegistrationCard(private val cards: CabinetCards) {
    suspend operator fun invoke(registerId: String): RegistrationCard = cards.card(registerId)
}
