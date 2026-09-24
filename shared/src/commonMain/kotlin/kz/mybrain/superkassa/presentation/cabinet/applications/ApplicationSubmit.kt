package kz.mybrain.superkassa.presentation.cabinet.applications

import kz.mybrain.superkassa.domain.cabinet.model.ApplicationStage
import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationSent
import kz.mybrain.superkassa.domain.cabinet.model.documents.DeregistrationRequest
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReregistrationRequest
import kz.mybrain.superkassa.presentation.cabinet.CabinetProblem
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts

/**
 * Что владелец подаёт в ИСНА и чем кончилась подача.
 *
 * Сама подача — подготовка, подпись, отправка — правило предметной области
 * (`CabinetApplications.submit`); здесь только то, что видит владелец.
 */

/** Как назван шаг подачи: каждый ждёт своего — кабинета, владельца с ключом, снова кабинета. */
fun ApplicationStage.title(texts: CabinetTexts): String = when (this) {
    ApplicationStage.Preparing -> texts.stagePreparing
    ApplicationStage.Signing -> texts.stageSigning
    ApplicationStage.Sending -> texts.stageSending
}

/** Чем закончилась подача: отправлено либо не отправлено — и почему. */
sealed interface ApplicationOutcome {
    data class Sent(val sent: ApplicationSent) : ApplicationOutcome
    data class Failed(val problem: CabinetProblem) : ApplicationOutcome
}

/** Что владелец подаёт в ИСНА. */
enum class ActionKind(val title: (CabinetTexts) -> String) {
    Registration({ it.registration }),
    Reregistration({ it.reregistration }),
    Deregistration({ it.deregistration })
}

/** Почему кассу снимают с учёта — набор задан ИСНА. */
enum class DeregistrationReason(val code: String, val title: (CabinetTexts) -> String) {
    CessationOfUse("CESSATION_OF_USE", { it.reasonCessation }),
    Broken("KKM_BROKEN", { it.reasonBroken }),
    Lost("KKM_LOST", { it.reasonLost }),
    Other("OTHER", { it.reasonOther })
}

/**
 * Заполненное владельцем в заявлении.
 *
 * @property placeId новая точка при перерегистрации.
 */
data class ApplicationForm(
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
