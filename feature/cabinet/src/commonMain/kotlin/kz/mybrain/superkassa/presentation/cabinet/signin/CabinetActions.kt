package kz.mybrain.superkassa.presentation.cabinet.signin

import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel

/**
 * Что владелец делает с кабинетом окна: входит, передумывает ждать
 * подписи, выходит. Действия по умолчанию пустые — для снимков вида,
 * где нажимать некому.
 */
internal interface CabinetActions {
    fun signIn() = Unit

    fun cancelSignIn() = Unit

    fun signOut() = Unit
}

/** Действия кабинета, выполняемые этой моделью. */
internal fun CabinetViewModel.actions(): CabinetActions {
    val model = this
    return object : CabinetActions {
        override fun signIn() = model.signIn()

        override fun cancelSignIn() = model.cancelSignIn()

        override fun signOut() = model.signOut()
    }
}

/** Действия кабинета для мастера: вход без чтения хозяйства сети. */
internal fun CabinetViewModel.actionsForOne(): CabinetActions {
    val model = this
    return object : CabinetActions {
        override fun signIn() = model.signIn(lists = false)

        override fun cancelSignIn() = model.cancelSignIn()

        override fun signOut() = model.signOut()
    }
}
