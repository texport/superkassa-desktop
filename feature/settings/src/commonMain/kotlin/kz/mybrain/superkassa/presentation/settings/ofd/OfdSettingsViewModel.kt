package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.lifecycle.ViewModel
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.settings.usecase.ofd.SyncWithBfd
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.presentation.settings.refusedByOfd
import kz.mybrain.superkassa.strings.api.textsOf

/** Что владелец делает со связью кассы. Пустые действия — для снимков вида. */
interface OfdSettingsActions {
    fun syncService() = Unit

    fun syncCounters() = Unit

    fun typeToken(text: String) = Unit

    fun saveToken() = Unit

    fun checkLink() = Unit

    fun askInfo() = Unit

    fun askNextRequest() = Unit
}

/**
 * Сверка кассы с БФД, замена токена ОФД и проверка связи.
 *
 * Всё, что касса знает о себе, приходит от БФД; разойтись они могут после
 * автономной работы или замены сведений в кабинете, и тогда кассир сверяет
 * их отсюда, а не переустанавливает кассу. Токен выдаёт ОФД и он же его
 * отзывает: без замены токена заблокированную кассу было не вернуть в строй.
 */
class OfdSettingsViewModel(private val cases: OfdCases, private val talk: Talk) : ViewModel(), OfdSettingsActions {
    private val screen = MutableStateFlow(OfdSettingsUiState())
    private val busy = Busy()

    val state: StateFlow<OfdSettingsUiState> = screen.asStateFlow()

    init {
        follow(cases.observe().map { it.kkm }.distinctUntilChanged()) { kkm ->
            screen.update {
                if (it.kkm?.kkmId == kkm?.kkmId) it.copy(kkm = kkm) else OfdSettingsUiState(kkm = kkm, busy = busy.now)
            }
        }
        follow(cases.observe().map { it.isAdmin }.distinctUntilChanged()) { admin ->
            screen.update { it.copy(admin = admin) }
        }
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
    }

    override fun syncService() = sync(SyncWithBfd.Part.Service, kkmTexts().run { syncService to syncServiceDone })

    override fun syncCounters() = sync(SyncWithBfd.Part.Counters, kkmTexts().run { syncCounters to syncCountersDone })

    override fun typeToken(text: String) = screen.update { it.copy(token = text.filter(Char::isDigit)) }

    override fun saveToken() {
        val token = screen.value.token.takeIf { it.isNotBlank() } ?: return
        whileBusy(busy) {
            val texts = textsOf(talk.language()).common.settings
            cases.token(token).shown(texts.saveToken, "update ofd token", talk) ?: return@whileBusy
            screen.update { it.copy(token = "") }
            talk.done(texts.tokenSaved)
        }
    }

    /** Отвечает ли ОФД: ответ — не состояние кассы, а итог только что нажатой проверки. */
    override fun checkLink() {
        if (screen.value.kkm == null) return
        whileBusy(busy) {
            val alive = cases.link().shown(textsOf(talk.language()).common.settings.ofdLink, "check ofd link", talk)
            alive?.let { screen.update { it.copy(linkAlive = alive) } }
        }
    }

    override fun askInfo() {
        whileBusy(busy) {
            val texts = textsOf(talk.language()).common.settings
            val summary = cases.info(talk.language().code).shown(texts.ofdInfo, "read ofd info", talk)
            if (summary != null) screen.update { it.copy(summary = summary) }
        }
    }

    /** Номер следующего запроса к БФД: им сверяют расхождения счёта запросов. */
    override fun askNextRequest() {
        whileBusy(busy) {
            val texts = textsOf(talk.language()).settings.facts
            val number = cases.nextRequest().shown(texts.ofdAuth, "read ofd request number", talk)
            if (number != null) screen.update { it.copy(nextRequest = number) }
        }
    }

    /**
     * Сверяет кассу с БФД.
     *
     * Касса отвечает итогом и тогда, когда БФД сверку не выполнил: такой
     * итог — отказ, и «сведения обновлены» поверх него было бы неправдой.
     */
    private fun sync(part: SyncWithBfd.Part, words: Pair<String, String>) {
        whileBusy(busy) {
            val answer = cases.sync(part).shown(words.first, SYNC, talk) ?: return@whileBusy
            if (answer.status == OfdCommandStatus.OK) {
                talk.done(words.second)
            } else {
                talk.refusedByOfd(words.first, SYNC, answer)
            }
        }
    }

    private companion object {
        /** Сверка с БФД — для журнала; удачная сверка снимает отказ прежней. */
        const val SYNC = "sync with ofd"
    }

    /** Надписи настройки кассы на языке кассира: ими названы сверки с БФД. */
    private fun kkmTexts() = textsOf(talk.language()).kassa.money.kkm
}
