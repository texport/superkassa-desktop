package kz.mybrain.superkassa.domain.cabinet.port

import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationPrepared
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationSent
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.domain.cabinet.model.documents.SignRequest

/** Заявления кассы: подготовка, отправка подписанного и след поданного. */
interface CabinetApplications {

    /** Кабинет готовит заявление и отдаёт то, что нужно подписать. */
    suspend fun prepare(application: CabinetApplication): ApplicationPrepared

    /** Подписанное уходит в ИСНА. */
    suspend fun send(application: CabinetApplication, sign: SignRequest): ApplicationSent

    /** Что с кассой делали: поданные заявления и ответы ИСНА. */
    suspend fun actions(registerId: String): List<RegistrationAction>
}
