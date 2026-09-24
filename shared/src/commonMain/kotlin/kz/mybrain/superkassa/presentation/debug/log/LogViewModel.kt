package kz.mybrain.superkassa.presentation.debug.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.strings.debug.debugTexts

/**
 * Журнал рабочего места: окно отладки и карточка «Режим отладки».
 *
 * Строки, порог и режим приходят из книги журнала сами — модель их
 * не перечитывает. Своё у модели только отбор окна.
 */
class LogViewModel(private val cases: LogCases, private val talk: Talk) : ViewModel(), LogActions {
    private val screen = MutableStateFlow(LogUiState())

    val state: StateFlow<LogUiState> = screen.asStateFlow()

    init {
        follow(cases.observe()) { book -> screen.update { it.copy(book = book) } }
    }

    override fun filter(level: LogLevel) = screen.update { it.copy(filter = level) }

    override fun search(query: String) = screen.update { it.copy(query = query) }

    override fun clear() = cases.clear()

    /** Сохраняется то, что видно после отбора: в поддержку пересылают разбор одного отказа. */
    override fun save() {
        val lines = screen.value.shown
        viewModelScope.launch { cases.save(lines, debugTexts(talk.language()).save) }
    }

    override fun chooseLevel(level: LogLevel) = cases.chooseLevel(level)

    override fun switchDebugMode(on: Boolean) = cases.switchDebugMode(on)
}
