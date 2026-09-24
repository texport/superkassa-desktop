package kz.mybrain.superkassa.domain.signin.usecase

import kotlinx.coroutines.flow.StateFlow
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.model.SignInState

/**
 * Кто за кассой — сейчас и при каждой смене.
 *
 * Единственный сценарий без `suspend`: он ничего не делает, а отдаёт поток,
 * на который модель подписывается сама. Модели областей следят за входом
 * только через него, а не через держатель входа.
 */
class ObserveSignIn(private val signIn: SignIn) {
    operator fun invoke(): StateFlow<SignInState> = signIn.state
}
