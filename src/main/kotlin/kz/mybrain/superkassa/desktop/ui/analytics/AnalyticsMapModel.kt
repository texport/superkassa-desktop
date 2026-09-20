package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.CabinetSession
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
        chosen = null
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
                chosen = null
            }
            .onFailure {
                view = null
                trouble = analyticsTrouble(it)
            }
        loading = false
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
