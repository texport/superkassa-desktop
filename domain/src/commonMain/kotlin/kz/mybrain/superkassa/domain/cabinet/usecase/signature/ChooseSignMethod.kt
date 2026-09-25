package kz.mybrain.superkassa.domain.cabinet.usecase.signature

import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.domain.cabinet.port.Signing

/** Владелец выбирает способ подписи. */
class ChooseSignMethod(private val signing: Signing) {
    operator fun invoke(method: SignMethod) = signing.choose(method)
}
