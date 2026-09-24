package kz.mybrain.superkassa.presentation.settings.tax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.strings.settings.coreSettingTexts

/**
 * Налоговый режим кассы, её ставка по умолчанию, автозакрытие и автоизъятие.
 *
 * Режим определяет, чем облагаются чеки этой кассы, а ставка по умолчанию —
 * с какой начинается каждая новая позиция. Касса принимает их только
 * в режиме программирования, при закрытой смене и пустой очереди:
 * сменить режим посреди смены значит получить в одном Z-отчёте чеки
 * с разными налогами.
 */
class TaxSettingsViewModel(private val cases: TaxCases, private val talk: Talk) : ViewModel(), TaxSettingsActions {
    private val screen = MutableStateFlow(TaxSettingsUiState())
    private val busy = Busy()

    val state: StateFlow<TaxSettingsUiState> = screen.asStateFlow()

    init {
        follow(cases.observe().map { it.kkm }.distinctUntilChanged(), ::onKkm)
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
    }

    override fun chooseRegime(code: String) = screen.update {
        it.copy(regimeDraft = code, vatDraft = if (code == TaxRegime.NO_VAT.name) VatGroup.NO_VAT.name else it.vatDraft)
    }

    override fun chooseVat(code: String) = screen.update { it.copy(vatDraft = code) }

    /**
     * Отправляет обе настройки одним обращением.
     *
     * Выбранное забывается только после согласия кассы: отказ оставляет
     * его на месте, иначе владелец выбирал бы заново.
     */
    override fun saveTax() {
        val now = screen.value
        val regime = now.regime?.let(::regimeOf) ?: return
        val group = now.vatGroup?.let(::groupOf) ?: return
        whileBusy(busy) {
            val texts = stringsOf(talk.language()).settings
            cases.save(regime, group).shown(texts.taxSettings, "update tax settings", talk) ?: return@whileBusy
            screen.update { it.copy(regimeDraft = null, vatDraft = null) }
            talk.done(texts.settingsSaved)
        }
    }

    /** Дочитывает справочники: касса могла не ответить, когда их спрашивали. */
    override fun retryDictionaries() {
        viewModelScope.launch { readDictionaries() }
    }

    override fun switchAutoClose(on: Boolean) =
        saveSwitches(coreSettingTexts(talk.language()).autoClose) { on to it.autoCashout }

    override fun switchAutoCashout(on: Boolean) =
        saveSwitches(stringsOf(talk.language()).settings.autoCashout) { it.autoCloseShift to on }

    /** Автозакрытие и автоизъятие касса меняет одним обращением: оба уходят каждый раз. */
    private fun saveSwitches(what: String, wanted: (KkmResponse) -> Pair<Boolean, Boolean>) {
        val (autoClose, autoCashout) = wanted(screen.value.kkm ?: return)
        whileBusy(busy) {
            cases.switches(autoClose, autoCashout).shown(what, "update kkm switches", talk) ?: return@whileBusy
            talk.done(stringsOf(talk.language()).settings.settingsSaved)
        }
    }

    /** Касса сменилась: выбранное для прежней к новой не относится. */
    private suspend fun onKkm(kkm: KkmResponse?) {
        screen.update { now ->
            if (now.kkm?.kkmId == kkm?.kkmId) now.copy(kkm = kkm) else TaxSettingsUiState(kkm = kkm, busy = busy.now)
        }
        if (kkm != null && !screen.value.dictionariesRead) readDictionaries()
    }

    /**
     * Справочники режимов и ставок — у кассы.
     *
     * Не прочитались — на месте полей стоит отказ с повтором, а не две
     * пустые рамки: владелец видел бы «Выбирать не из чего», не понимая,
     * у кого ничего нет.
     */
    private suspend fun readDictionaries() {
        val read = cases.dictionaries() as? Answer.Done ?: return
        screen.update { it.copy(regimes = read.value.regimes, vatRates = read.value.vatRates, dictionariesRead = true) }
    }
}

private fun regimeOf(code: String): TaxRegime? = TaxRegime.entries.firstOrNull { it.name == code }

private fun groupOf(code: String): VatGroup? = VatGroup.entries.firstOrNull { it.name == code }
