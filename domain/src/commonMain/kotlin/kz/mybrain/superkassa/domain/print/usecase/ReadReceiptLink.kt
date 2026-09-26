package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.model.drawerPin
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Ссылка на электронный чек у БФД — её отдают покупателю вместе с файлом.
 *
 * Ссылку знает касса: она приходит в ответе БФД на принятый чек. У чека,
 * который БФД ещё не принял, и у документа кабинета, переданного пакетом,
 * ссылки здесь нет — делятся одним файлом.
 */
class ReadReceiptLink(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @param entered пин, введённый владельцем ради формы; у вошедшего кассира не нужен. */
    suspend operator fun invoke(source: PrintSource, kkmId: String, entered: String?): String? {
        val pin = signed.drawerPin(kkmId, entered)
        if (source !is PrintSource.Journal || pin == null) return null
        val details = kassa.ask { it.getDocumentDetails(kkmId, source.documentId, pin) }
        return (details as? Answer.Done)?.value?.document?.receiptUrl?.takeIf { it.isNotBlank() }
    }
}
