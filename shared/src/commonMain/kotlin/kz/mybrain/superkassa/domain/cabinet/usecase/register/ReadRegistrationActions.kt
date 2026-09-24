package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.domain.cabinet.port.CabinetApplications

/** Что с кассой делали: поданные заявления и ответы ИСНА. */
class ReadRegistrationActions(private val applications: CabinetApplications) {
    suspend operator fun invoke(id: String): List<RegistrationAction> = applications.actions(id)
}
