package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.ApplicationPrepared
import kz.mybrain.superkassa.desktop.server.cabinet.ApplicationSent
import kz.mybrain.superkassa.desktop.server.cabinet.DeregistrationRequest
import kz.mybrain.superkassa.desktop.server.cabinet.ReregistrationRequest
import kz.mybrain.superkassa.desktop.server.cabinet.SignRequest
import kz.mybrain.superkassa.desktop.server.cabinet.prepareDeregistration
import kz.mybrain.superkassa.desktop.server.cabinet.prepareRegistration
import kz.mybrain.superkassa.desktop.server.cabinet.prepareReregistration
import kz.mybrain.superkassa.desktop.server.cabinet.signDeregistration
import kz.mybrain.superkassa.desktop.server.cabinet.signRegistration
import kz.mybrain.superkassa.desktop.server.cabinet.signReregistration
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Подача заявления в ИСНА: подготовка, подпись, отправка.
 *
 * Между подготовкой и отправкой стоит владелец с ключом: если он закроет
 * окно NCALayer, заявление останется черновиком в кабинете и его можно
 * подать заново — фискального следа это не оставляет.
 */
suspend fun submitApplication(
    cabinet: CabinetSession,
    kind: ActionKind,
    registerId: String,
    placeId: String,
    reason: DeregistrationReason,
    comment: String,
    onStage: (ApplicationStage) -> Unit = {}
): ApplicationOutcome {
    val token = cabinet.token ?: return ApplicationOutcome.Failed(CabinetProblem.SessionExpired)
    val client = cabinet.client
    val sent = cabinet.guard {
        onStage(ApplicationStage.Preparing)
        val prepared: ApplicationPrepared = when (kind) {
            ActionKind.Registration -> client.prepareRegistration(token, registerId)
            ActionKind.Reregistration -> client.prepareReregistration(
                token,
                registerId,
                ReregistrationRequest(newRetailPlaceId = placeId.trim().takeIf { it.isNotBlank() })
            )
            ActionKind.Deregistration -> client.prepareDeregistration(
                token,
                registerId,
                DeregistrationRequest(reason = reason.code, comment = comment.trim().takeIf { it.isNotBlank() })
            )
        }
        onStage(ApplicationStage.Signing)
        val sign = SignRequest(prepared.actionId, cabinet.sign(prepared.payloadToSign))
        onStage(ApplicationStage.Sending)
        when (kind) {
            ActionKind.Registration -> client.signRegistration(token, registerId, sign)
            ActionKind.Reregistration -> client.signReregistration(token, registerId, sign)
            ActionKind.Deregistration -> client.signDeregistration(token, registerId, sign)
        }
    }
    // Помеха читается сразу, пока каркас окна не забрал её во всплывающую строку:
    // владелец должен видеть причину под кнопкой, а не ловить её три секунды.
    return sent?.let(ApplicationOutcome::Sent)
        ?: ApplicationOutcome.Failed(cabinet.problem ?: CabinetProblem.SignDeclined(""))
}

/** На каком шаге подача: каждый ждёт своего — кабинета, владельца с ключом, снова кабинета. */
enum class ApplicationStage(val title: (CabinetTexts) -> String) {
    Preparing({ it.stagePreparing }),
    Signing({ it.stageSigning }),
    Sending({ it.stageSending })
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
