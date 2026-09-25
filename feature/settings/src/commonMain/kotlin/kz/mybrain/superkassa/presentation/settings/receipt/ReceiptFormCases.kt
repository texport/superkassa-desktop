package kz.mybrain.superkassa.presentation.settings.receipt

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.settings.usecase.receipt.ReadPaperWidths
import kz.mybrain.superkassa.domain.settings.usecase.receipt.SaveReceiptForm
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/** Сценарии печатной формы кассы. */
class ReceiptFormCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val paperWidths = ReadPaperWidths(kassa)
    val save = SaveReceiptForm(kassa, signIn)
}
