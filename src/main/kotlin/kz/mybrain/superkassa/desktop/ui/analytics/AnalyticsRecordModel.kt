package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.server.cabinet.cashRegisterMap

/**
 * Состояние вкладки учёта: что спрошено у кабинета и что из этого вышло.
 *
 * Спрашивается та же ручка, что и у карты: в ответе о кассах на карте
 * уже лежит всё, из чего складывается учёт, — состояние КГД, блокировка,
 * смена, торговая точка и её адрес. Своей ручки ради тех же полей
 * заводить не за что, а второе обращение к кабинету за теми же кассами
 * стоило бы владельцу ещё одного ожидания.
 *
 * Источник положения здесь один и не выбирается: точка на карте вкладке
 * не нужна вовсе, а адрес торговой точки — тот самый, из которого
 * складывается регион. Отдельный ответ на вкладку, а не общий с картой,
 * потому что вкладки живут порознь: открыв учёт, владелец не должен
 * ждать, пока карта найдёт три тысячи домов.
 */
class AnalyticsRecordModel(private val cabinet: CabinetSession) {

    var view: KkmMapView? by mutableStateOf(null)
        private set

    var trouble: AnalyticsTrouble? by mutableStateOf(null)
        private set

    var loading: Boolean by mutableStateOf(false)
        private set

    /** Спрашивает кабинет о кассах компании. */
    suspend fun load() {
        val token = cabinet.token ?: return
        loading = true
        trouble = null
        askedCabinet { cabinet.client.cashRegisterMap(token, PositionSource.RetailPlaceAddress) }
            .onSuccess {
                view = it
                trouble = null
            }
            .onFailure {
                view = null
                trouble = analyticsTrouble(it)
            }
        loading = false
    }
}
