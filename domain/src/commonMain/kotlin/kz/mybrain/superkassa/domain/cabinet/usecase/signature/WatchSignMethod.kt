package kz.mybrain.superkassa.domain.cabinet.usecase.signature

import kotlinx.coroutines.flow.StateFlow
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.domain.cabinet.port.Signing

/** Каким способом владелец подписывает сейчас. */
class WatchSignMethod(private val signing: Signing) {
    operator fun invoke(): StateFlow<SignMethod> = signing.method
}
