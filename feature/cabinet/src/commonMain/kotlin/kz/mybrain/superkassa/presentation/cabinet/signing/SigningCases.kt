package kz.mybrain.superkassa.presentation.cabinet.signing

import kz.mybrain.superkassa.domain.cabinet.port.Signing
import kz.mybrain.superkassa.domain.cabinet.usecase.signature.AnswerSignRequest
import kz.mybrain.superkassa.domain.cabinet.usecase.signature.ChooseSignMethod
import kz.mybrain.superkassa.domain.cabinet.usecase.signature.ReadSignMethods
import kz.mybrain.superkassa.domain.cabinet.usecase.signature.WatchSignMethod
import kz.mybrain.superkassa.domain.cabinet.usecase.signature.WatchSignRequest

/** Сценарии подписи: способы, выбор способа и ответы подписывающему. */
class SigningCases(signing: Signing) {
    val methods = ReadSignMethods(signing)
    val method = WatchSignMethod(signing)
    val choose = ChooseSignMethod(signing)
    val request = WatchSignRequest(signing)
    val answer = AnswerSignRequest(signing)
}
