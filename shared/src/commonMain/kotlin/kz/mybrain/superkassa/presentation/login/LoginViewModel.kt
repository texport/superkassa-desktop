package kz.mybrain.superkassa.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.ask
import kz.mybrain.superkassa.domain.signin.Pin
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.messages.shown
import kz.mybrain.superkassa.presentation.strings.stringsOf

/**
 * Вход кассира: список касс, выбор, пин и проверка пина кассой.
 *
 * Пин проверяется сразу: без проверки кассир узнавал бы о неверном пине
 * только на первом чеке — в очереди у кассы, с покупателем перед ним.
 * Удачный вход записывается в держатель входа, и окно само переходит
 * к работе: экрану входа об этом знать незачем.
 */
class LoginViewModel(private val app: AppContainer) : ViewModel() {
    private val screen = MutableStateFlow(LoginUiState())

    val state: StateFlow<LoginUiState> = screen.asStateFlow()

    /** Перечитывает список касс. Итог прошлого действия перечитывание не стирает. */
    fun reload() {
        viewModelScope.launch { read() }
    }

    /** Кассир набирает номер или название: прежний выбор мышью отменяется. */
    fun search(text: String) {
        screen.update { it.copy(search = text, pickedId = null) }
    }

    fun pick(kkm: KkmResponse) {
        screen.update { it.copy(pickedId = kkm.kkmId) }
    }

    fun typePin(text: String) {
        screen.update { it.copy(pin = Pin.digitsOf(text)) }
    }

    fun open(door: Door) {
        screen.update { it.copy(door = door) }
    }

    /** Проверяет пин у кассы и начинает работу; неверный пин — отказ словами кассы. */
    fun enter() {
        val now = screen.value
        val kkm = now.chosen ?: return
        if (now.entering || !Pin.enterable(now.pin)) return
        screen.update { it.copy(entering = true) }
        viewModelScope.launch { enterWith(kkm, now.pin) }
    }

    private suspend fun read() {
        val texts = stringsOf(app.language())
        val list = app.kassa.ask { it.listKkms(KkmListParams(limit = MAX_KKMS)) }
            .shown(texts.login.reload, "read kkm list", app)
        val kkms = list?.items ?: screen.value.kkms
        screen.update { now ->
            now.copy(
                kkms = kkms,
                localNames = kkms.mapNotNull { kkm -> app.memory.localName(kkm.kkmId)?.let { kkm.kkmId to it } }
                    .toMap(),
                answered = true,
                listRead = list != null || now.listRead,
                rememberedId = app.memory.rememberedKkmId,
                selectedId = app.signIn.state.value.kkm?.kkmId
            )
        }
    }

    private suspend fun enterWith(kkm: KkmResponse, pin: String) {
        app.notices.clear()
        val texts = stringsOf(app.language())
        val cashier = app.kassa.ask { it.authenticate(kkm.kkmId, pin) }
            .shown(texts.login.enter, "sign in", app)
        screen.update { it.copy(entering = false, pin = if (cashier == null) it.pin else "") }
        cashier ?: return
        app.memory.rememberedKkmId = kkm.kkmId
        app.journal.info("signed in to kkm ${kkm.kkmId} as ${cashier.role}")
        app.signIn.enter(kkm, cashier, pin)
    }

    private companion object {
        /** Столько касс на одном рабочем месте не бывает; больше ядро не отдаёт за раз. */
        const val MAX_KKMS = 1000
    }
}
