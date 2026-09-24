package kz.mybrain.superkassa.domain.signin.usecase

import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.signin.model.SignIn

/**
 * Кассир уходит, касса остаётся.
 *
 * Касса на рабочем месте одна и та же, а кассиры за смену меняются:
 * искать её в списке заново новому кассиру незачем. Пин забывается
 * сразу — им больше никто не работает.
 */
class SignOut(private val signIn: SignIn, private val journal: Journal) {
    suspend operator fun invoke() {
        if (!signIn.state.value.signedIn) return
        journal.info("cashier signed out")
        signIn.signOut()
    }
}
