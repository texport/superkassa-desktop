package kz.mybrain.superkassa.presentation.login

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse

/**
 * Что кассир может сделать на входе.
 *
 * Экран получает действия готовыми: поле зовёт действие, модель решает.
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface LoginActions {
    fun reload() = Unit

    fun search(text: String) = Unit

    fun pick(kkm: KkmResponse) = Unit

    fun typePin(text: String) = Unit

    fun enter() = Unit

    fun open(door: Door) = Unit
}

/** Действия входа, выполняемые этой моделью. */
fun LoginViewModel.actions(): LoginActions {
    val model = this
    return object : LoginActions {
        override fun reload() = model.reload()

        override fun search(text: String) = model.search(text)

        override fun pick(kkm: KkmResponse) = model.pick(kkm)

        override fun typePin(text: String) = model.typePin(text)

        override fun enter() = model.enter()

        override fun open(door: Door) = model.open(door)
    }
}
