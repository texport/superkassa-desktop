package kz.mybrain.superkassa.presentation.journal.documents

import kz.mybrain.superkassa.domain.document.usecase.ReadDocumentTypes
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.journal.usecase.ReadPeriodDocuments
import kz.mybrain.superkassa.domain.journal.usecase.ReadReceiptDelivery
import kz.mybrain.superkassa.domain.journal.usecase.ResendReceipt
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/**
 * Сценарии журнала документов за срок: касса и пин — у сценариев, модель их не видит.
 *
 * Доставка чека покупателю идёт своим портом: ядро отдаёт её отдельно
 * от кассовых операций.
 */
class JournalCases(kassa: Kassa, signIn: SignIn, ports: JournalPorts) {
    val observe = ObserveSignIn(signIn)
    val readDocumentTypes = ReadDocumentTypes(kassa)
    val readPeriodDocuments = ReadPeriodDocuments(kassa, signIn)
    val readDelivery = ReadReceiptDelivery(ports.deliveries, signIn)
    val resendReceipt = ResendReceipt(ports.deliveries, signIn)
}
