package kz.mybrain.superkassa.presentation.kassa.cash

import kz.mybrain.superkassa.domain.document.usecase.ReadDocumentTypes
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.usecase.MoveCash
import kz.mybrain.superkassa.domain.kassa.usecase.ReadDrawer
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/**
 * Сценарии денежного ящика: всё, что модель делает с кассой, — через них.
 *
 * Касса и пин команды — у сценариев, модель их не видит.
 */
class CashCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val move = MoveCash(kassa, signIn)
    val readDrawer = ReadDrawer(kassa, signIn)
    val documentTypes = ReadDocumentTypes(kassa)
}
