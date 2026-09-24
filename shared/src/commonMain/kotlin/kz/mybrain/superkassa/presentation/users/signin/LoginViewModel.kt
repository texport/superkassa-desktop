package kz.mybrain.superkassa.presentation.users.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.Pin
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.strings.common.stringsOf

/**
 * Вход кассира: список касс, выбор, пин и проверка пина кассой.
 *
 * Пин проверяется сразу: без проверки кассир узнавал бы о неверном пине
 * только на первом чеке — в очереди у кассы, с покупателем перед ним.
 * Удачный вход записывается в держатель входа, и окно само переходит
 * к работе: экрану входа об этом знать незачем.
 */
class LoginViewModel(private val cases: LoginCases, private val talk: Talk) : ViewModel(), LoginActions {
    private val screen = MutableStateFlow(LoginUiState())

    val state: StateFlow<LoginUiState> = screen.asStateFlow()

    /** Перечитывает список касс. Итог прошлого действия перечитывание не стирает. */
    override fun reload() {
        viewModelScope.launch { read() }
    }

    /** Кассир набирает номер или название: прежний выбор мышью отменяется. */
    override fun search(text: String) {
        screen.update { it.copy(search = text, pickedId = null) }
    }

    override fun pick(kkm: KkmResponse) {
        screen.update { it.copy(pickedId = kkm.kkmId) }
    }

    override fun typePin(text: String) {
        screen.update { it.copy(pin = Pin.digitsOf(text)) }
    }

    override fun open(door: Door) {
        screen.update { it.copy(door = door) }
    }

    /** Проверяет пин у кассы и начинает работу; неверный пин — отказ словами кассы. */
    override fun enter() {
        val now = screen.value
        val kkm = now.chosen ?: return
        if (now.entering || !Pin.enterable(now.pin)) return
        screen.update { it.copy(entering = true) }
        viewModelScope.launch { enterWith(kkm, now.pin) }
    }

    /**
     * Кассир уходит, касса остаётся.
     *
     * Строка сообщений снимается: отказ, оставшийся от ушедшего кассира,
     * новому ни о чём не говорит.
     */
    fun signOut() {
        viewModelScope.launch {
            talk.clear()
            cases.signOut()
        }
    }

    private suspend fun read() {
        val choice = cases.readKkms(screen.value.kkms)
        val read = choice.answer.shown(stringsOf(talk.language()).login.reload, "read kkm list", talk) != null
        screen.update { now ->
            now.copy(
                kkms = choice.kkms,
                localNames = choice.localNames,
                answered = true,
                listRead = read || now.listRead,
                rememberedId = choice.rememberedId,
                selectedId = cases.observe().value.kkm?.kkmId
            )
        }
    }

    private suspend fun enterWith(kkm: KkmResponse, pin: String) {
        talk.clear()
        val answer = cases.signInCashier(kkm, pin)
        answer.shown(stringsOf(talk.language()).login.enter, "sign in", talk)
        screen.update { it.copy(entering = false, pin = if (answer is Answer.Done) "" else it.pin) }
    }
}
