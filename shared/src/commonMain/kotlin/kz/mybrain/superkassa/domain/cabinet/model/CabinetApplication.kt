package kz.mybrain.superkassa.domain.cabinet.model

import kz.mybrain.superkassa.domain.cabinet.model.documents.DeregistrationRequest
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReregistrationRequest

/** Заявление в ИСНА о кассе: постановка, перерегистрация или снятие с учёта. */
sealed interface CabinetApplication {
    val registerId: String

    data class Registration(override val registerId: String) : CabinetApplication

    data class Reregistration(override val registerId: String, val request: ReregistrationRequest) :
        CabinetApplication

    data class Deregistration(override val registerId: String, val request: DeregistrationRequest) :
        CabinetApplication
}

/** На каком шаге подача: каждый ждёт своего — кабинета, владельца с ключом, снова кабинета. */
enum class ApplicationStage { Preparing, Signing, Sending }
