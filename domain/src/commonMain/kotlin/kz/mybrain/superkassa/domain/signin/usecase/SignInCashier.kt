package kz.mybrain.superkassa.domain.signin.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Вход кассира: пин проверяет касса сразу.
 *
 * Без проверки кассир узнавал бы о неверном пине только на первом чеке —
 * в очереди у кассы, с покупателем перед ним. Принятый вход записывается
 * в держатель входа, и касса запоминается до следующего запуска.
 */
class SignInCashier(
    private val kassa: Kassa,
    private val signIn: SignIn,
    private val memory: WorkplaceMemory,
    private val journal: Journal
) {
    suspend operator fun invoke(kkm: KkmResponse, pin: String): Answer<UserResponse> {
        val answer = kassa.ask { it.authenticate(kkm.kkmId, pin) }
        if (answer is Answer.Done) {
            memory.rememberedKkmId = kkm.kkmId
            journal.info("signed in to kkm ${kkm.kkmId} as ${answer.value.role}")
            signIn.enter(kkm, answer.value, pin)
        }
        return answer
    }
}
