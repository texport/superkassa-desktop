package kz.mybrain.superkassa.domain.users.usecase

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserUpdateRequest
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.users.model.UserRules

/**
 * Задаёт кассиру новый пин.
 *
 * Пин меняют и себе, и другим, и последствия разные: сменивший пин себе
 * продолжает работу новым пином, сменивший его другому — своим прежним.
 * Новый пин принадлежит тому, кому его задали, и чеки работающего под ним
 * не подписываются никогда.
 */
class ChangeCashierPin(private val kassa: Kassa, private val signIn: SignIn) {

    /** @return сменён ли пин самому работающему; `null` — менять нельзя. */
    suspend operator fun invoke(user: UserResponse, newPin: String): Answer<Boolean>? {
        val seat = signIn.state.value
        val kkm = seat.kkm?.kkmId?.takeIf { seat.signedIn && UserRules.pinAccepted(newPin) } ?: return null
        val own = UserRules.same(seat.cashier, user)
        return kassa.ask { it.updateUser(kkm, user.userId, seat.pin, UserUpdateRequest(userPin = newPin)) }
            .map { own }
            .also { answer -> if (own && answer is Answer.Done) signIn.changePin(newPin) }
    }
}
