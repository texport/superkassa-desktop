package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.presentation.analytics.common.OwnerAccess
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.common.model.latest

/**
 * Список адресов обмена, поиск по нему и отбор по кассе.
 *
 * Список спрашивается целиком по компании; отбор по кассе и поиск
 * работают уже по нему — см. [AnalyticsExchangeUiState.rows].
 *
 * В журнал приложения адреса не пишутся: это служебное сведение о том,
 * откуда выходят на связь машины владельца, и место ему на экране.
 */
class AnalyticsExchangeViewModel(private val cases: ExchangeCases) : ViewModel() {
    private val screen = MutableStateFlow(AnalyticsExchangeUiState())
    private val access = OwnerAccess()
    private val reading = latest()

    val state: StateFlow<AnalyticsExchangeUiState> = screen.asStateFlow()

    /** Вошёл другой владелец или вышел этот: прежний ответ не его. */
    fun follow(owner: String?) {
        if (!access.changed(owner)) return
        screen.value = AnalyticsExchangeUiState()
        refresh()
    }

    /** Спрашивает кабинет об адресах обмена заново. */
    fun refresh() {
        if (access.current == null) return
        reading.restart {
            screen.update { it.copy(reading = it.reading.started()) }
            val answer = Reading.of(cases.read())
            screen.update { it.copy(reading = answer) }
        }
    }

    /** Строка поиска по адресам и кассам. */
    fun search(query: String) = screen.update { it.copy(query = query) }

    /** Отбор по кассе; `null` — все кассы. */
    fun pick(register: String?) = screen.update { it.copy(register = register) }
}
