package kz.mybrain.superkassa.presentation.journal.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.signin.model.SignInState
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.followSeat
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Очередь отложенной отправки выбранной кассы.
 *
 * Касса перечитывается вместе с очередью: повтор она принимает только
 * в режиме программирования, и кнопка без свежего состояния кассы
 * обещала бы то, чего касса не сделает.
 */
class QueueViewModel(private val cases: QueueCases, private val talk: Talk) : ViewModel(), QueueActions {
    private val screen = MutableStateFlow(QueueUiState())
    private val busy = Busy()
    private val reading = latest()
    private val texts: CommonTexts get() = textsOf(talk.language()).common

    val state: StateFlow<QueueUiState> = screen.asStateFlow()

    init {
        followSeat(cases.observe(), each = { signIn -> screen.update { it.copy(kkm = signIn.kkm) } }, seated = ::reseat)
        follow(busy.active) { on -> screen.update { it.copy(retrying = on) } }
        viewModelScope.launch {
            cases.readDocumentTypes()?.let { types -> screen.update { it.copy(documentTypes = types) } }
        }
    }

    /** Перечитывает очередь; за кассой никого нет — спрашивать некого. */
    override fun refresh() {
        if (cases.observe().value.signedIn) reading.restart { read() }
    }

    /** Возвращает неудавшиеся задачи в очередь; итог объявляется до перечитывания. */
    override fun retryFailed() {
        if (!screen.value.canRetry) return
        whileBusy(busy) {
            val retried = cases.retryFailed()?.shown(texts.queue.retryFailed, "retry queue", talk)
            if (retried != null) talk.done(texts.queue.retryDone)
            read()
        }
    }

    /** За кассой сел другой: прочитанная очередь принадлежала прежнему. */
    private fun reseat(signIn: SignInState) {
        reading.cancel()
        screen.update { QueueUiState(kkm = it.kkm, documentTypes = it.documentTypes, retrying = busy.now) }
        // Очередь — раздел администратора, а модель окна переживает смену
        // кассира: за кассира кассу об очереди не спрашивают.
        if (signIn.signedIn && signIn.isAdmin) refresh()
    }

    /**
     * Перечитывает кассу и её очередь.
     *
     * Касса, не ответившая об очереди, прежнего ответа не отменяет: строки
     * остаются, а непрочитанной считается только очередь, о которой
     * не сказано ни разу.
     */
    private suspend fun read() {
        screen.update { it.copy(reading = true) }
        val tasks = cases.readQueue().shown(texts.queue.title, "read queue", talk)
        screen.update {
            if (tasks == null) it.copy(reading = false) else it.copy(tasks = tasks, read = true, reading = false)
        }
    }
}
