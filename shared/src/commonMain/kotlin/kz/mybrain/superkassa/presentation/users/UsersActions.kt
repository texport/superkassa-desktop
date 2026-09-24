package kz.mybrain.superkassa.presentation.users

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse

/**
 * Что администратор может сделать с кассирами.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface UsersActions {
    fun reload() = Unit

    fun edit(form: CashierForm) = Unit

    fun create() = Unit

    /** Открывает смену пина кассиру; `null` — закрывает окно. */
    fun askPin(user: UserResponse?) = Unit

    fun typeNewPin(text: String) = Unit

    fun confirmPin() = Unit

    /** Спрашивает об удалении кассира; `null` — снимает вопрос. */
    fun askRemove(user: UserResponse?) = Unit

    fun confirmRemove() = Unit
}
