package kz.mybrain.superkassa.presentation.cabinet.applications

import kz.mybrain.superkassa.domain.cabinet.model.ApplicationStage
import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationSent
import kz.mybrain.superkassa.domain.cabinet.model.documents.DeregistrationRequest
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReregistrationRequest
import kz.mybrain.superkassa.presentation.cabinet.CabinetProblem
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Что владелец подаёт в ИСНА и чем кончилась подача.
 *
 * Сама подача — подготовка, подпись, отправка — правило предметной области
 * (`CabinetApplications.submit`); здесь только то, что видит владелец.
 */

/** Как назван шаг подачи: каждый ждёт своего — кабинета, владельца с ключом, снова кабинета. */
internal fun ApplicationStage.title(texts: CabinetTexts): String = when (this) {
    ApplicationStage.Preparing -> texts.applications.stagePreparing
    ApplicationStage.Signing -> texts.applications.stageSigning
    ApplicationStage.Sending -> texts.applications.stageSending
}

/** Чем закончилась подача: отправлено либо не отправлено — и почему. */
internal sealed interface ApplicationOutcome {
    data class Sent(val sent: ApplicationSent) : ApplicationOutcome
    data class Failed(val problem: CabinetProblem) : ApplicationOutcome
}

/** Что владелец подаёт в ИСНА. */
internal enum class ActionKind(val title: (CabinetTexts) -> String) {
    Registration({ it.applications.registration }),
    Reregistration({ it.applications.reregistration }),
    Deregistration({ it.applications.deregistration })
}

/** Почему кассу снимают с учёта — набор задан ИСНА. */
internal enum class DeregistrationReason(val code: String, val title: (CabinetTexts) -> String) {
    CessationOfUse("CESSATION_OF_USE", { it.applications.reasonCessation }),
    Broken("KKM_BROKEN", { it.applications.reasonBroken }),
    Lost("KKM_LOST", { it.applications.reasonLost }),
    Other("OTHER", { it.applications.reasonOther })
}

/**
 * Заполненное владельцем в заявлении.
 *
 * @property placeId новая точка при перерегистрации.
 */
internal data class ApplicationForm(
    val kind: ActionKind = ActionKind.Registration,
    val reason: DeregistrationReason = DeregistrationReason.CessationOfUse,
    val comment: String = "",
    val placeId: String = ""
) {
    /**
     * Заявление, которого кабинет не примет, и не подаётся: перерегистрация
     * без новой точки уходила в кабинет и возвращалась отказом.
     */
    val filled: Boolean get() = kind != ActionKind.Reregistration || placeId.isNotBlank()

    /** Заявление о кассе [registerId] в том виде, в каком его ждёт кабинет. */
    fun application(registerId: String): CabinetApplication = when (kind) {
        ActionKind.Registration -> CabinetApplication.Registration(registerId)
        ActionKind.Reregistration -> CabinetApplication.Reregistration(
            registerId,
            ReregistrationRequest(newRetailPlaceId = placeId.trim().takeIf { it.isNotBlank() })
        )
        ActionKind.Deregistration -> CabinetApplication.Deregistration(
            registerId,
            DeregistrationRequest(reason = reason.code, comment = comment.trim().takeIf { it.isNotBlank() })
        )
    }
}
