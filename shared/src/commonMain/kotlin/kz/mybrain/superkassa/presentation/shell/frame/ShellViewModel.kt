package kz.mybrain.superkassa.presentation.shell.frame

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.kkm.model.displayName
import kz.mybrain.superkassa.domain.signin.model.SignInState
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.whileBusy

/**
 * Каркас окна: вход или рабочее окно, какая касса в шапке и кто за ней.
 *
 * Один на окно. Касса и кассир — со слов держателя входа: каркас их
 * не выбирает и не меняет, а только показывает. Первое состояние — уже
 * сделанный вход: окно не рисует экран входа поверх вошедшего кассира.
 */
class ShellViewModel(private val cases: ShellCases, private val talk: Talk) : ViewModel() {
    private val busy = Busy()
    private val screen = MutableStateFlow(seated(cases.observe().value))

    val state: StateFlow<ShellUiState> = screen.asStateFlow()

    init {
        follow(cases.observe()) { seat -> screen.update { seated(seat).copy(busy = it.busy) } }
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
    }

    /**
     * Перечитывает кассу: её состояние видят шапка и все разделы.
     *
     * Касса, которая не ответила, остаётся прежней — перечитывание
     * ничего не отменяет, а отказ кассы ему объяснять некому.
     */
    fun refresh() {
        whileBusy(busy) { cases.refresh() }
    }

    /**
     * Кассир сам ушёл в другой раздел: итог прежнего действия к новому
     * экрану не относится и снимается.
     */
    fun sectionPicked() = talk.clear()

    private fun seated(seat: SignInState) = ShellUiState(
        seat = seat,
        kkmName = seat.kkm?.let { kkm -> kkm.displayName(cases.localName(kkm.kkmId)) }
    )
}
