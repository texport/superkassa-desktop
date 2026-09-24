package kz.mybrain.superkassa.presentation.cabinet.signin

import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel

/**
 * Что владелец делает с кабинетом окна: входит, передумывает ждать
 * подписи, выходит. Действия по умолчанию пустые — для снимков вида,
 * где нажимать некому.
 */
interface CabinetActions {
    fun signIn() = Unit

    fun cancelSignIn() = Unit

    fun signOut() = Unit
}

/** Действия кабинета, выполняемые этой моделью. */
fun CabinetViewModel.actions(): CabinetActions {
    val model = this
    return object : CabinetActions {
        override fun signIn() = model.signIn()

        override fun cancelSignIn() = model.cancelSignIn()

        override fun signOut() = model.signOut()
    }
}
