package kz.mybrain.superkassa.domain.users.usecase

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/** Названия ролей со слов кассы; касса промолчала — `null`, и экран назовёт их сам. */
class ReadRoleNames(private val kassa: Kassa) {
    suspend operator fun invoke(): Map<String, TrilingualMessageResponse>? =
        (kassa.ask { it.getUserRoles() } as? Answer.Done)?.value?.associate { it.code to it.name }
}
