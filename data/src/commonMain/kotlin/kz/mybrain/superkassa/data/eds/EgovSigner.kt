package kz.mybrain.superkassa.data.eds

import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.integrations.egovmobile.EgovDocument
import kz.mybrain.superkassa.integrations.egovmobile.EgovJournal
import kz.mybrain.superkassa.integrations.egovmobile.EgovMobile
import kz.mybrain.superkassa.integrations.egovmobile.EgovReason
import kz.mybrain.superkassa.integrations.egovmobile.EgovRefusal
import kz.mybrain.superkassa.integrations.egovmobile.EgovSettings

/**
 * Подпись ключом владельца в приложении eGov mobile.
 *
 * Протокол и посредник — в модуле `egov-mobile`; здесь процедура становится
 * просьбой на столе подписи ([SignRequest.EgovMobile]: QR и ссылка запуска),
 * а отказ eGov mobile — отказом подписи предметной области.
 *
 * @param document как подпись называется в eGov mobile — на языке владельца
 *   в момент подписи.
 */
class EgovSigner(
    private val egov: EgovMobile,
    private val desk: SignDesk,
    private val document: () -> EgovDocument
) : Signer {

    override suspend fun sign(payload: String): String = try {
        val procedure = egov.open(payload, document())
        desk.showing(SignRequest.EgovMobile(procedure.launch, procedure.qr, procedure.expiresAt)) {
            egov.await(procedure)
        }
    } catch (refusal: EgovRefusal) {
        throw refusal.eds()
    }

    /** Сборка подписывающего. */
    companion object {
        /** eGov mobile через посредника по умолчанию; ожидание — столько же, сколько у всякой подписи. */
        fun open(desk: SignDesk, journal: Journal, document: () -> EgovDocument): EgovSigner =
            EgovSigner(EgovMobile(EgovSettings(signWindow = Signer.SIGN_WINDOW), egovJournal(journal)), desk, document)
    }
}

/** Отказ eGov mobile — отказом подписи: что случилось, кодом для слов владельца. */
internal fun EgovRefusal.eds(): EdsRefusal = when (reason) {
    EgovReason.Unreachable -> EdsRefusal(EdsProblem.Declined, Signer.EGOV_UNREACHABLE, this)
    EgovReason.Cancelled -> EdsRefusal(EdsProblem.Declined, Signer.CANCELLED, this)
    EgovReason.Expired -> EdsRefusal(EdsProblem.Declined, Signer.EGOV_EXPIRED, this)
    EgovReason.Refused -> EdsRefusal(EdsProblem.Declined, detail, this)
}

/** Ход обмена с посредником — в журнал приложения, без данных и подписи. */
private fun egovJournal(journal: Journal): EgovJournal = EgovJournal { line, failure ->
    if (failure == null) {
        journal.info("egov mobile: $line")
    } else {
        journal.warn("egov mobile: $line, ${failure::class.simpleName}")
    }
}
