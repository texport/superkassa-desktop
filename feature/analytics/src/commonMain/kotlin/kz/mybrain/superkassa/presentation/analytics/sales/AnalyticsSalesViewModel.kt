package kz.mybrain.superkassa.presentation.analytics.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kz.mybrain.superkassa.presentation.analytics.common.OwnerAccess
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kotlin.time.Duration

/**
 * Торговая сводка: сеть целиком или одна касса.
 *
 * Разрезы кабинета спрашиваются разом сценарием сводки, и частичный ответ
 * не показывается вовсе — помеха одна на весь экран.
 *
 * Срок по умолчанию — неделя: владелец открывает раздел, чтобы посмотреть,
 * как торговали на этой неделе, а не за один сегодняшний день.
 *
 * @param register касса, которой ограничен отбор; `null` — вся сеть.
 *   Тот же расчёт и те же ручки: сводка по одной кассе отличается
 *   от сводки по сети только этим отбором.
 */
internal class AnalyticsSalesViewModel(private val cases: SalesCases, val register: String? = null) : ViewModel() {
    private val screen = MutableStateFlow(AnalyticsSalesUiState(JournalPeriod.of(JournalSpan.Week, cases.today())))
    private val access = OwnerAccess()
    private val reading = latest()

    val state: StateFlow<AnalyticsSalesUiState> = screen.asStateFlow()

    /** Вошёл другой владелец или вышел этот: его сводки у модели больше нет. */
    fun follow(owner: String?) {
        if (!access.changed(owner)) return
        cases.readSales.forget()
        screen.update { AnalyticsSalesUiState(it.period) }
        refresh()
    }

    /** Другой срок: сводка читается заново целиком. */
    fun choose(period: JournalPeriod) {
        if (period == screen.value.period) return
        screen.update { it.copy(period = period) }
        refresh()
    }

    /** Спрашивает кабинет о сводке выбранного срока заново. */
    fun refresh() {
        if (access.current == null) return
        val range = screen.value.period.range
        val from = range?.from
        val to = range?.to
        reading.restart {
            screen.update { it.copy(reading = it.reading.started()) }
            val answer = register?.let { cases.readKkmSales(it, from, to) } ?: cases.readSales(from, to)
            screen.update { it.copy(reading = Reading.of(answer)) }
        }
    }

    /**
     * Перечитывает сводку сама, пока жива модель — окно кассы открыто.
     *
     * Касса торгует сейчас, и сводка, снятая при открытии, к концу разговора
     * о ней уже неверна. Своего потока нет: ожидание живёт в той корутине,
     * что его позвала, и кончается вместе с окном.
     */
    suspend fun watch(every: Duration) {
        while (viewModelScope.isActive) {
            delay(every)
            refresh()
        }
    }
}
