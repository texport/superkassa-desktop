package kz.mybrain.superkassa.presentation.shift.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.signin.model.SignInState
import kz.mybrain.superkassa.domain.signin.model.sameSeat
import kz.mybrain.superkassa.presentation.common.message.deliveryReport
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
 * Главный экран: смена выбранной кассы, её документы и действия над сменой.
 *
 * Модель живёт, пока открыто окно, а не пока виден экран: кассир уходит
 * в продажу и возвращается к тому же состоянию, без повторного чтения.
 * Сменился кассир или касса — состояние сбрасывается и читается заново.
 */
class DashboardViewModel(private val cases: DashboardCases, private val talk: Talk) : ViewModel() {
    private val screen = MutableStateFlow(DashboardUiState())
    private val busy = Busy()
    private val reading = latest()
    private val texts: CommonTexts get() = textsOf(talk.language()).common

    val state: StateFlow<DashboardUiState> = screen.asStateFlow()

    init {
        followSeat(
            cases.observe(),
            each = { signIn -> screen.update { it.copy(kkm = signIn.kkm, isAdmin = signIn.isAdmin) } },
            seated = ::reseat
        )
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
        viewModelScope.launch {
            cases.readDocumentTypes()?.let { types -> screen.update { it.copy(documentTypes = types) } }
        }
    }

    /**
     * Перечитывает кассу. Зовётся и кнопкой, и открытием экрана: чек,
     * пробитый в продаже, должен стоять в документах смены. Идущее
     * чтение второй раз не начинается.
     */
    fun refresh() {
        reading.startIfIdle { read() }
    }

    fun openShift() = act(texts.dashboard.openShift, "open shift", texts.dashboard.shiftOpened) {
        cases.openShift().map { null }
    }

    fun xReport() = act(texts.dashboard.xReport, "x report", texts.dashboard.xReportDone) { cases.xReport() }

    /** Z-отчёт: смена закрывается, и он не отменяется — спрашивает экран до вызова. */
    fun closeShift() {
        val words = texts.dashboard
        act(words.closeShift, "close shift", words.shiftClosedDone) { cases.closeShift() }
    }

    /** Есть ли связь с ОФД: ответ объявляется и когда связи нет — кассир нажал и ждёт итога. */
    fun checkLink() = thenRead {
        cases.checkLink().shown(texts.autonomous.checkLink, "check ofd link", talk)?.let { alive ->
            talk.done(if (alive) texts.autonomous.linkBack else texts.settingsScreen.ofdSilent)
        }
    }

    /** Досылает в ОФД то, что ждёт в очереди. */
    fun sendQueued() = thenRead {
        val sent = cases.sendQueued().shown(texts.queue.retryFailed, "retry queue", talk)
        if (sent != null) talk.done(texts.queue.retryDone)
    }

    /**
     * Действие над сменой: кнопки заняты, пока касса отвечает и перечитывается.
     *
     * Итог действия объявляется до перечитывания, и беда перечитывания
     * его не перебивает: кассир спрашивал, что сделала кнопка.
     */
    private fun act(doing: String, action: String, done: String, request: suspend () -> Answer<DeliveryStatus?>) {
        if (screen.value.kkm == null) return
        whileBusy(busy) {
            talk.clear()
            val answer = request()
            if (answer is Answer.Done) {
                talk.done(deliveryReport(done, answer.value, texts), action)
            } else {
                answer.shown(doing, action, talk)
            }
            read()
        }
    }

    /** Действие над выбранной кассой без занятости кнопок; после него касса перечитывается. */
    private fun thenRead(block: suspend () -> Unit) {
        if (screen.value.kkm == null) return
        viewModelScope.launch {
            block()
            read()
        }
    }

    /** За кассой сел другой: прочитанное принадлежало прежнему. */
    private fun reseat(signIn: SignInState) {
        reading.cancel()
        screen.value = DashboardUiState(
            signIn.kkm,
            signIn.isAdmin,
            documentTypes = screen.value.documentTypes,
            busy = busy.now
        )
        if (signIn.signedIn) reading.restart { read() }
    }

    /** Касса, прочитанная для прежнего кассира, новому не показывается. */
    private suspend fun read() {
        val seated = cases.observe().value
        screen.update { it.copy(reading = seated.signedIn) }
        val snapshot = cases.readShift()?.takeIf { seated.sameSeat(cases.observe().value) } ?: return
        screen.update { it.adopt(snapshot) }
        snapshot.trouble?.takeIf { talk.silent }?.let { trouble ->
            trouble.answer.shown(trouble.words(texts, talk.language()), trouble.action, talk)
        }
        val operators = cases.readOperators(screen.value.refused, screen.value.operators)
        screen.update { it.copy(operators = it.operators + operators) }
    }
}
