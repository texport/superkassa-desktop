package kz.mybrain.superkassa.domain.cabinet.usecase.signature

import kotlinx.coroutines.flow.StateFlow
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.Signing

/** Что подписывающий просит показать владельцу сейчас. */
class WatchSignRequest(private val signing: Signing) {
    operator fun invoke(): StateFlow<SignRequest?> = signing.desk.request
}
