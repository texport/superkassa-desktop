package kz.mybrain.superkassa.domain.cabinet.usecase.applications

import kz.mybrain.superkassa.domain.cabinet.model.ApplicationStage
import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationSent
import kz.mybrain.superkassa.domain.cabinet.model.documents.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.CabinetApplications
import kz.mybrain.superkassa.domain.cabinet.port.Signer

/**
 * Подача заявления в ИСНА: подготовка, подпись владельца, отправка.
 *
 * Все три вида идут одним путём, различаются только тела. Между подготовкой
 * и отправкой стоит владелец с ключом: если он закроет окно подписи,
 * заявление останется черновиком в кабинете и его можно подать заново —
 * фискального следа это не оставляет.
 */
class SubmitApplication(private val applications: CabinetApplications, private val signer: Signer) {

    /** @param onStage какой шаг начался: экран показывает ожидание своего шага. */
    suspend operator fun invoke(application: CabinetApplication, onStage: (ApplicationStage) -> Unit): ApplicationSent {
        onStage(ApplicationStage.Preparing)
        val prepared = applications.prepare(application)
        onStage(ApplicationStage.Signing)
        val signature = signer.sign(prepared.payloadToSign)
        onStage(ApplicationStage.Sending)
        return applications.send(application, SignRequest(prepared.actionId, signature))
    }
}
