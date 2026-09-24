package kz.mybrain.superkassa.domain.users.usecase

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.users.model.UserRules

/**
 * Удаляет кассира.
 *
 * Удаливший себя выходит из кассы: касса вместе с кассиром забыла и его
 * пин, и дальше на любое действие приходил бы отказ, а экран показывал
 * бы вошедшим того, кого на кассе уже нет.
 */
class RemoveCashier(private val kassa: Kassa, private val signIn: SignIn) {

    /** @return удалил ли работающий себя; `null` — удалять некому. */
    suspend operator fun invoke(user: UserResponse): Answer<Boolean>? {
        val seat = signIn.state.value
        val kkm = seat.kkm?.kkmId?.takeIf { seat.signedIn } ?: return null
        val self = UserRules.same(seat.cashier, user)
        return kassa.ask { it.deleteUser(kkm, user.userId, seat.pin) }
            .map { self }
            .also { answer -> if (self && answer is Answer.Done) signIn.signOut() }
    }
}
