package kz.mybrain.superkassa.presentation.shell.frame

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.signin.model.SignInState

/**
 * Что каркас окна знает о кассе и кассире.
 *
 * @property seat кто за кассой: от него зависит, вход или рабочее окно.
 * @property kkmName как касса зовётся на этом рабочем месте; `null` — кассы нет.
 * @property busy касса перечитывается по кнопке шапки.
 */
data class ShellUiState(
    val seat: SignInState = SignInState(),
    val kkmName: String? = null,
    val busy: Boolean = false
) {
    val kkm: KkmResponse? get() = seat.kkm

    /** Имя вошедшего кассира; `null` — никто не вошёл. */
    val cashier: String? get() = seat.cashier?.name?.takeIf { seat.signedIn }
}
