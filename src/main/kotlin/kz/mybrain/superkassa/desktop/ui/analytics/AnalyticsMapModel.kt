package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.server.cabinet.cashRegisterMap
import kz.mybrain.superkassa.desktop.ui.map.MapGeocoder
import kz.mybrain.superkassa.desktop.ui.map.MapState

/**
 * Состояние карты касс: что спрошено у кабинета и что из этого вышло.
 *
 * Обращение идёт мимо общей обёртки сеанса кабинета намеренно. Та
 * сводит любую неудачу к одной помехе и показывает её всплывающей
 * строкой; здесь же `404` означает не отказ, а то, что раздел кабинета
 * ещё не выложен, и сказать об этом нужно на самом экране.
 */
class AnalyticsMapModel(private val cabinet: CabinetSession, geocoder: MapGeocoder) {

    /** Откуда брать положение касс. */
    var source: PositionSource by mutableStateOf(PositionSource.RetailPlaceAddress)
        private set

    var view: KkmMapView? by mutableStateOf(null)
        private set

    var trouble: AnalyticsTrouble? by mutableStateOf(null)
        private set

    var loading: Boolean by mutableStateOf(false)
        private set

    /** Касса, карточку которой сейчас читают. */
    var chosen: String? by mutableStateOf(null)

    /**
     * Раскрытое место: ярлычок, список касс которого стоит под картой.
     *
     * Отдельно от выбранной кассы, а не вместо неё: из списка места
     * заходят в кассу, и вернуться к соседям по месту владелец должен
     * без нового поиска по карте.
     */
    var spot: String? by mutableStateOf(null)

    /** Касса, аналитику которой открыли отдельным окном. */
    var opened: AnalyticsKkm? by mutableStateOf(null)

    /** Отбор касс: он сужает и карту, и список рядом с ней. */
    var sieve: MapSieve by mutableStateOf(MapSieve())

    /** Где стоит карта. Пересоздаётся, когда набор касс сменился целиком. */
    var map: MapState by mutableStateOf(MapState())
        private set

    /** Координаты адресов торговых точек — их ищет карта, а не кабинет. */
    val points = AnalyticsAddressPoints(geocoder)

    private var centred = false

    /** Смена источника: спрошено будет заново, а выбранная касса сбрасывается. */
    fun choose(value: PositionSource) {
        if (value == source) return
        source = value
        forget()
    }

    /** Спрашивает кабинет о кассах при выбранном источнике положения. */
    suspend fun load() {
        val token = cabinet.token ?: return
        loading = true
        trouble = null
        centred = false
        askedCabinet { cabinet.client.cashRegisterMap(token, source) }
            .onSuccess {
                view = it
                forget()
            }
            .onFailure {
                view = null
                trouble = analyticsTrouble(it)
            }
        loading = false
    }

    /**
     * Забывает выбранное.
     *
     * Нужно и при новом запросе — прежней кассы в ответе может не быть, —
     * и при нажатии мимо ярлычка: раскрытое место закрывается тем же
     * способом, каким открылось.
     */
    fun forget() {
        chosen = null
        spot = null
    }

    /**
     * Раскрывает место, нажатое на карте.
     *
     * Место с одной кассой сразу открывает её карточку: заставлять
     * владельца нажать дважды там, где выбор один, незачем.
     */
    fun open(group: KkmGroup) {
        spot = group.id
        chosen = group.kkms.singleOrNull()?.kkm?.cashRegisterId
        map.glideTo(group.latitude, group.longitude)
    }

    /**
     * Ведёт карту к кассе, выбранной в списке рядом.
     *
     * Вместе с кассой раскрывается и её место: касса могла оказаться
     * в ярлычке с соседями, и вернуться к ним владелец должен без
     * нового поиска по карте.
     */
    fun show(row: PlacedKkm, groups: List<KkmGroup>) {
        chosen = row.kkm.cashRegisterId
        spot = groups.firstOrNull { it.holds(row.kkm.cashRegisterId) }?.id
        map.glideTo(row.latitude, row.longitude)
    }

    /** Ищет на карте адреса тех касс, координат которых кабинет не дал. */
    suspend fun findAddresses() {
        points.resolve(addressesToFind(view))
    }

    /**
     * Ведёт карту к кассам — один раз на загрузку.
     *
     * Иначе карта возвращалась бы в середину набора после каждого
     * найденного адреса, и владелец не мог бы её сдвинуть.
     */
    fun centre(placed: List<PlacedKkm>) {
        if (centred) return
        val fit = fitting(placed, FIT_WIDTH, FIT_HEIGHT) ?: return
        map = MapState(fit.latitude, fit.longitude, fit.zoom)
        centred = true
    }
}
