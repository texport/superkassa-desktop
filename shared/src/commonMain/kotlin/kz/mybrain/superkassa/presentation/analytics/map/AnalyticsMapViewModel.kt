package kz.mybrain.superkassa.presentation.analytics.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.analytics.model.AddressAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.addressesToFind
import kz.mybrain.superkassa.domain.analytics.model.placement
import kz.mybrain.superkassa.presentation.analytics.common.OwnerAccess
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.common.mapview.MapState
import kz.mybrain.superkassa.presentation.common.model.latest

/**
 * Карта касс: что спрошено у кабинета, где кассы встали и что выбрал владелец.
 *
 * При источнике «адрес торговой точки» координат кабинет не даёт вовсе,
 * и дома ищет служба карт — по одному, с паузой, которую держит она сама;
 * найденный прежде дом снова не спрашивается.
 * Найденное ложится в состояние по мере прихода, и карта ведётся к кассам,
 * пока владелец не распорядился ею сам.
 */
class AnalyticsMapViewModel(private val cases: KkmMapCases) : ViewModel(), AnalyticsMapActions {
    private val screen = MutableStateFlow(AnalyticsMapUiState())
    private val access = OwnerAccess()
    private val reading = latest()

    val state: StateFlow<AnalyticsMapUiState> = screen.asStateFlow()

    fun follow(owner: String?) {
        if (!access.changed(owner)) return
        screen.update { AnalyticsMapUiState(source = it.source) }
        refresh()
    }

    /** Смена источника: спрошено будет заново, а выбранная касса сбрасывается. */
    override fun choose(source: PositionSource) {
        if (source == screen.value.source) return
        screen.update { it.copy(source = source).forgotten() }
        refresh()
    }

    /**
     * Спрашивает кабинет о кассах и ищет дома тех, кому кабинет точки
     * не дал. Новый ответ — новое наведение: карта встаёт как при первом
     * открытии, и рука владельца, двигавшая прежний набор, ему не мешает.
     */
    override fun refresh() {
        if (access.current == null) return
        val source = screen.value.source
        reading.restart {
            screen.update { it.copy(reading = it.reading.started()) }
            val answer = cases.read(source)
            screen.update { it.copy(reading = Reading.of(answer), map = MapState(), steered = false).forgotten() }
            centre()
            if (answer !is AnalyticsAnswer.Done) return@restart
            cases.map.findHouses(addressesToFind(answer.value), screen.value.foundAddresses) { address, place ->
                val found = place?.let { AddressAnswer.Found(it.latitude, it.longitude) } ?: AddressAnswer.Missing
                screen.update { it.copy(found = it.found + (address to found)) }
                centre()
            }
        }
    }

    override fun sift(sieve: MapSieve) = screen.update { it.copy(sieve = sieve) }

    /**
     * Раскрывает место, нажатое на карте. Место с одной кассой сразу
     * открывает её карточку: нажимать дважды там, где выбор один, незачем.
     */
    override fun open(group: KkmGroup) {
        val single = group.kkms.singleOrNull()?.kkm?.cashRegisterId
        screen.update { it.copy(spot = group.id, chosen = single, steered = true) }
        screen.value.map.glideTo(group.latitude, group.longitude)
    }

    /**
     * Ведёт карту к кассе, выбранной в списке рядом. Вместе с кассой
     * раскрывается и её место: касса могла оказаться в ярлычке с соседями.
     */
    override fun show(row: PlacedKkm, groups: List<KkmGroup>) {
        val id = row.kkm.cashRegisterId
        screen.update { it.copy(chosen = id, spot = groups.firstOrNull { g -> g.holds(id) }?.id, steered = true) }
        screen.value.map.glideTo(row.latitude, row.longitude)
    }

    /** Касса, выбранная в списке места; `null` — назад к соседям по месту. */
    override fun pick(kkm: String?) = screen.update { it.copy(chosen = kkm) }

    /** Нажатие мимо ярлычка: раскрытое место закрывается тем же способом, каким открылось. */
    override fun forget() = screen.update { it.forgotten() }

    /** Окно аналитики одной кассы; `null` — закрыть. */
    override fun openSales(kkm: AnalyticsKkm?) = screen.update { it.copy(opened = kkm) }

    /**
     * Держит весь набор в окне и отъезжает по мере его роста — пока
     * владелец не распорядился картой сам: перетаскиванием, колесом,
     * кнопками увеличения, выбором кассы или ярлычка.
     */
    private fun centre() {
        val now = screen.value
        if (now.steered || now.map.steered) return
        val fit = fitting(placement(now.reading.value, now).placed, FIT_WIDTH, FIT_HEIGHT) ?: return
        now.map.centreOn(fit.latitude, fit.longitude, fit.zoom)
    }
}
