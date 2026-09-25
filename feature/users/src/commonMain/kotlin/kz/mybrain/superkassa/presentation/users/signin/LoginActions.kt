package kz.mybrain.superkassa.presentation.users.signin

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse

/**
 * Что кассир может сделать на входе.
 *
 * Экран получает действия готовыми: поле зовёт действие, модель решает.
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому;
 * модель входа выполняет их сама.
 */
interface LoginActions {
    fun reload() = Unit

    fun search(text: String) = Unit

    fun pick(kkm: KkmResponse) = Unit

    fun typePin(text: String) = Unit

    fun enter() = Unit
}
