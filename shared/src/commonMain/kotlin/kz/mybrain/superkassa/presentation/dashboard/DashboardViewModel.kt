package kz.mybrain.superkassa.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.Answer
import kz.mybrain.superkassa.domain.kassa.ask
import kz.mybrain.superkassa.domain.signin.SignInState
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.messages.Message
import kz.mybrain.superkassa.presentation.messages.shown
import kz.mybrain.superkassa.presentation.strings.AppStrings
import kz.mybrain.superkassa.presentation.strings.stringsOf

/**
 * Главный экран: смена выбранной кассы, её документы и действия над сменой.
 *
 * Модель живёт, пока открыто окно, а не пока виден экран: кассир уходит
 * в продажу и возвращается к тому же состоянию, без повторного чтения.
 * Сменился кассир или касса — состояние сбрасывается и читается заново.
 */
class DashboardViewModel(private val app: AppContainer) : ViewModel() {
    private val screen = MutableStateFlow(DashboardUiState())
    private val reader = ShiftReader(app)
    private var seat: Pair<String?, String>? = null
    private var reading: Job? = null
    private val texts: AppStrings get() = stringsOf(app.language())

    val state: StateFlow<DashboardUiState> = screen.asStateFlow()

    init {
        viewModelScope.launch { app.signIn.state.collect(::follow) }
        viewModelScope.launch {
            reader.documentTypes()?.let { types -> screen.update { it.copy(documentTypes = types) } }
        }
    }

    fun refresh() {
        viewModelScope.launch { read() }
    }

    fun openShift() = act(texts.dashboard.openShift, "open shift", texts.dashboard.shiftOpened) { api, kkm, pin ->
        api.openShift(kkm, pin).let { null }
    }

    fun xReport() = act(texts.dashboard.xReport, "x report", texts.dashboard.xReportDone) { api, kkm, pin ->
        api.createReport(kkm, pin).deliveryStatus
    }

    /** Z-отчёт: смена закрывается, и он не отменяется — спрашивает экран до вызова. */
    fun closeShift() {
        val words = texts.dashboard
        act(words.closeShift, "close shift", words.shiftClosedDone) { api, kkm, pin ->
            api.closeShift(kkm, pin).deliveryStatus
        }
    }

    /** Есть ли связь с ОФД: ответ объявляется и когда связи нет — кассир нажал и ждёт итога. */
    fun checkLink() {
        launchOnKkm { kkm, _ ->
            val answer = app.kassa.ask { it.checkOfdConnection(kkm) }
            answer.shown(texts.autonomous.checkLink, "check ofd link", app)?.let {
                val alive = it.status == OfdCommandStatus.OK
                app.notices.show(Message.Done(if (alive) texts.autonomous.linkBack else texts.settings.ofdSilent))
            }
        }
    }

    /** Досылает в ОФД то, что ждёт в очереди. */
    fun sendQueued() {
        launchOnKkm { kkm, pin ->
            app.kassa.ask { it.queue.retryFailed(kkm, pin) }.shown(texts.queue.retryFailed, "retry queue", app)
                ?.let { app.notices.show(Message.Done(texts.queue.retryDone)) }
        }
    }

    private fun act(
        doing: String,
        action: String,
        done: String,
        request: (SuperkassaApi, String, String) -> DeliveryStatus?
    ) {
        if (screen.value.busy) return
        screen.update { it.copy(busy = true) }
        launchOnKkm { kkm, pin ->
            app.notices.clear()
            val answer = app.kassa.ask { request(it, kkm, pin) }
            if (answer is Answer.Done) {
                app.journal.info("$action done on kkm $kkm")
                app.notices.show(Message.Done(deliveryReport(done, answer.value, texts)))
            } else {
                answer.shown(doing, action, app)
            }
        }.invokeOnCompletion { screen.update { it.copy(busy = false) } }
    }

    /**
     * Действие над выбранной кассой; после него касса перечитывается.
     *
     * Итог действия объявляется до перечитывания, и беда перечитывания
     * его не перебивает: кассир спрашивал, что сделала кнопка.
     */
    private fun launchOnKkm(block: suspend (String, String) -> Unit): Job {
        val now = app.signIn.state.value
        val kkm = now.kkm ?: return Job().apply { complete() }
        return viewModelScope.launch {
            block(kkm.kkmId, now.pin)
            read()
        }
    }

    private fun follow(signIn: SignInState) {
        screen.update { it.copy(kkm = signIn.kkm, isAdmin = signIn.isAdmin) }
        val now = signIn.kkm?.kkmId to signIn.pin
        if (now == seat) return
        seat = now
        reading?.cancel()
        screen.value = DashboardUiState(signIn.kkm, signIn.isAdmin, documentTypes = screen.value.documentTypes)
        if (signIn.signedIn) reading = viewModelScope.launch { read() }
    }

    private suspend fun read() {
        val now = app.signIn.state.value
        val kkm = now.kkm?.takeIf { now.signedIn } ?: return
        screen.update { it.copy(reading = true) }
        val snapshot = reader.read(kkm.kkmId, now.pin, texts)
        if (app.signIn.state.value.let { it.kkm?.kkmId != kkm.kkmId || it.pin != now.pin }) return
        snapshot.kkm?.let(app.signIn::refresh)
        screen.update { it.adopt(snapshot) }
        snapshot.trouble?.takeIf { app.notices.last == null }?.let { it.answer.shown(it.what, it.action, app) }
        val operators = reader.operators(kkm.kkmId, now.pin, screen.value.refused, screen.value.operators)
        screen.update { it.copy(operators = it.operators + operators) }
    }
}
