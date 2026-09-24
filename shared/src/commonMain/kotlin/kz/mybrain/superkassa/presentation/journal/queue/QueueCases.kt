package kz.mybrain.superkassa.presentation.journal.queue

import kz.mybrain.superkassa.domain.document.usecase.ReadDocumentTypes
import kz.mybrain.superkassa.domain.journal.usecase.ReadQueue
import kz.mybrain.superkassa.domain.journal.usecase.RetryFailedQueue
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/**
 * Сценарии очереди отложенной отправки: всё, что модель делает с кассой, — только через них.
 *
 * Касса и пин команды — у сценариев, модель их не видит.
 */
class QueueCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val readDocumentTypes = ReadDocumentTypes(kassa)
    val readQueue = ReadQueue(kassa, signIn)
    val retryFailed = RetryFailedQueue(kassa, signIn)
}
