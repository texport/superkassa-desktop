package kz.mybrain.superkassa.presentation.settings.kkm

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
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts

/**
 * Сама касса в настройках: её название, режим программирования и снятие.
 *
 * Название уходит в кассу, режим меняет касса, снимает себя тоже она —
 * модель только передаёт команду и объявляет итог после согласия кассы.
 */
class KkmSettingsViewModel(private val cases: KkmCases, private val talk: Talk) : ViewModel(), KkmSettingsActions {
    private val screen = MutableStateFlow(KkmSettingsUiState())
    private val busy = Busy()

    val state: StateFlow<KkmSettingsUiState> = screen.asStateFlow()

    init {
        follow(cases.observe()) { signed -> screen.update { it.following(signed, cases.localName::invoke) } }
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
    }

    override fun typeName(text: String) = screen.update { it.copy(nameDraft = text) }

    /** Сохраняет набранное название; пустое поле снимает название. */
    override fun saveName() = rename(screen.value.nameField.takeIf { it.isNotBlank() })

    /** Возвращает кассе название, данное ей в БФД или по номеру. */
    override fun resetName() = rename(null)

    /**
     * Уводит на выбор кассы.
     *
     * Смена кассы — не переключатель на месте: иначе кассир меняет кассу,
     * не заметив, и пробивает чек не на той машине.
     */
    override fun switchKkm() {
        viewModelScope.launch { cases.switchKkm() }
    }

    override fun switchProgramming() {
        val enter = !screen.value.programming
        whileBusy(busy) {
            val texts = stringsOf(talk.language()).settings
            val action = if (enter) "enter programming" else "exit programming"
            val changed = cases.programming(enter).shown(texts.programmingMode, action, talk)
            if (changed != null) talk.done(if (enter) texts.enteredProgramming else texts.exitedProgramming)
        }
    }

    override fun askDecommission() = screen.update { it.copy(decommissionAsked = true) }

    override fun cancelDecommission() = screen.update { it.copy(decommissionAsked = false) }

    /**
     * Снимает кассу с этого рабочего места и уводит на выбор кассы.
     *
     * Оставаться в настройках удалённой кассы нельзя: каждое следующее
     * обращение отвечало бы «касса не найдена».
     */
    override fun decommission() {
        cancelDecommission()
        whileBusy(busy) {
            val texts = moneyTexts(talk.language()).kkm
            cases.decommission().shown(texts.decommission, "decommission kkm", talk) ?: return@whileBusy
            talk.done(texts.decommissionDone, "decommission kkm")
        }
    }

    /**
     * Название уходит в кассу; не принятое кассой остаётся хотя бы здесь.
     *
     * Своё название снимается при удаче: иначе оно перекрывало бы только
     * что записанное в кассу, и правка выглядела бы непринятой.
     */
    private fun rename(name: String?) {
        val kkmId = screen.value.kkm?.kkmId ?: return
        whileBusy(busy) {
            val texts = stringsOf(talk.language())
            val money = moneyTexts(talk.language()).kkm
            val saved = cases.rename(name).shown(texts.settings.localName, "rename kkm", talk)
            val local = cases.localName(kkmId)
            screen.update { it.copy(localName = local, nameDraft = if (saved == null) it.nameDraft else null) }
            if (saved != null) talk.done(if (name == null) money.renameReset else money.renameSaved)
        }
    }
}

/** Касса или кассир сменились: набранное принадлежало прежней кассе. */
private fun KkmSettingsUiState.following(signed: SignInState, localName: (String) -> String?): KkmSettingsUiState {
    val kkm = signed.kkm
    val same = this.kkm?.kkmId == kkm?.kkmId
    return KkmSettingsUiState(
        kkm = kkm,
        admin = signed.isAdmin,
        localName = kkm?.let { localName(it.kkmId) },
        nameDraft = nameDraft.takeIf { same },
        busy = busy
    )
}
