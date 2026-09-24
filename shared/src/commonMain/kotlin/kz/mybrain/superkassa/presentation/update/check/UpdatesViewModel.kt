package kz.mybrain.superkassa.presentation.update.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.update.model.AvailableUpdate
import kz.mybrain.superkassa.domain.update.model.InstallOutcome
import kz.mybrain.superkassa.domain.update.model.UpdateOutcome
import kz.mybrain.superkassa.domain.update.model.after
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.presentation.strings.update.updateTexts
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Проверка выпусков кассы: по расписанию и по нажатию.
 *
 * Что считается новой версией и когда пора проверять, решают сценарии
 * обновлений; модель держит найденное и итог ручной проверки. Найденное
 * показывается в углу окна и в настройках, а ставит кассир руками.
 *
 * Отказ сети при проверке по расписанию не показывается: кассир в нём
 * не виноват и сделать с ним ничего не может. Он остаётся в журнале.
 */
class UpdatesViewModel(private val cases: UpdatesCases, private val talk: Talk) : ViewModel(), UpdatesActions {
    private val screen = MutableStateFlow(
        cases.schedule().let { UpdatesUiState(cases.installed, lastChecked = it.lastChecked, automatic = it.automatic) }
    )
    private val checks = Busy()
    private val installing = Busy()

    val state: StateFlow<UpdatesUiState> = screen.asStateFlow()

    init {
        follow(checks.active) { on -> screen.update { it.copy(checking = on) } }
        follow(installing.active) { on -> screen.update { it.copy(installing = on) } }
        viewModelScope.launch { watch() }
    }

    override fun switchAutomatic(on: Boolean) {
        cases.switchAutomatic(on)
        screen.update { it.copy(automatic = on) }
    }

    /** Проверка по нажатию; итог остаётся под кнопкой, пока его читают. */
    override fun check() {
        viewModelScope.launch {
            val outcome = checkOnce()
            screen.update { it.copy(outcome = outcome) }
        }
    }

    /** Скачать, сверить и открыть установщик; второе нажатие во время скачивания ничего не начинает. */
    override fun install(update: AvailableUpdate) {
        whileBusy(installing) {
            talk.clear()
            val texts = updateTexts(talk.language())
            when (cases.install(update)) {
                InstallOutcome.Started -> talk.done(texts.installerOpened, INSTALL)
                InstallOutcome.PageOpened -> talk.done(texts.pageOpened)
                InstallOutcome.Tampered -> talk.say(INSTALL, Message.Refusal(texts.installerTampered, TAMPERED))
                InstallOutcome.Failed -> talk.say(INSTALL, Message.Failed(texts.download))
            }
        }
    }

    /**
     * Проверка по расписанию: через паузу после запуска и дальше, когда пора.
     *
     * Пауза — чтобы не спорить за сеть со входом кассира и загрузкой касс.
     */
    private suspend fun watch() {
        delay(START_DELAY)
        while (true) {
            if (cases.schedule().due) checkOnce()
            delay(WAKE_EVERY)
        }
    }

    /** Ручная во время фоновой не гасит признак раньше срока: занятость — счётчик. */
    private suspend fun checkOnce(): UpdateOutcome = checks.during {
        val outcome = cases.check()
        val checked = cases.schedule().lastChecked
        screen.update { it.copy(available = outcome.after(it.available), lastChecked = checked) }
        outcome
    }

    private companion object {
        /** Пауза после запуска: вход кассира важнее вопроса о выпусках. */
        val START_DELAY = 30.seconds

        /** Как часто просыпаться и смотреть, не пора ли. */
        val WAKE_EVERY = 1.hours

        const val INSTALL = "install update"

        /** Код отказа для журнала и строки: установщик не совпал с выпуском. */
        const val TAMPERED = "INSTALLER_CHECKSUM"
    }
}
