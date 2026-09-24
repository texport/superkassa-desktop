package kz.mybrain.superkassa.data.cabinet.applications

import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationPrepared
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationSent
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.domain.cabinet.model.documents.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.CabinetApplications
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.ApplicationsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.RegistrationAction as BfdAction
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.SignRequest as BfdSign

/** Заявления о регистрации, перерегистрации и снятии кассы — модулем кабинета. */
internal class RemoteApplications(private val applications: ApplicationsApi) : CabinetApplications {
    override suspend fun prepare(application: CabinetApplication): ApplicationPrepared {
        val prepared = cabinetCall { applications.prepare(application.sent()) }
        return ApplicationPrepared(prepared.actionId, prepared.actionType, prepared.payloadToSign, prepared.expiresAt)
    }

    override suspend fun send(application: CabinetApplication, sign: SignRequest): ApplicationSent {
        val sent = cabinetCall { applications.send(application.sent(), BfdSign(sign.actionId, sign.signatureCms)) }
        return ApplicationSent(sent.actionId, sent.actionStatus, sent.cashRegisterStatus, sent.externalRequestId)
    }

    override suspend fun actions(registerId: String): List<RegistrationAction> =
        cabinetCall { applications.actions(registerId) }.map { it.action() }
}

private fun BfdAction.action() = RegistrationAction(
    id = id,
    actionType = actionType,
    status = status,
    externalRequestId = externalRequestId,
    registrationNumber = registrationNumber,
    reasonCode = reasonCode,
    reasonMessage = reasonMessage,
    createdAt = createdAt,
    sentAt = sentAt,
    processedAt = processedAt,
    stateSyncStatus = stateSyncStatus
)
