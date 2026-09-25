package kz.mybrain.superkassa.domain.cabinet.usecase.signature

import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.domain.cabinet.port.Signing

/** Какими способами можно подписать здесь; первый — по умолчанию. */
class ReadSignMethods(private val signing: Signing) {
    operator fun invoke(): List<SignMethod> = signing.methods
}
