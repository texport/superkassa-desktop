package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.domain.map.model.SelfPlace
import kz.mybrain.superkassa.domain.map.usecase.AnswerLocationAsk
import kz.mybrain.superkassa.domain.map.usecase.LocateSelf

/**
 * Кнопка «Где я» на карте: идёт ли определение и нужен ли вопрос владельцу.
 *
 * Где искать — сперва служба самой машины, потом адрес подключения —
 * решает сценарий [LocateSelf]; здесь только то, что видно на карте.
 * Метку выбранной точки определение не ставит: своё место — это своё
 * место, а точку выбирает владелец нажатием.
 */
class MapLocating(private val locateSelf: LocateSelf, private val answerAsk: AnswerLocationAsk) {

    /** Идёт определение: кнопка погашена, второе нажатие ничего не добавит. */
    var busy: Boolean by mutableStateOf(false)
        private set

    /** Нужен вопрос владельцу о запасном пути. */
    var asking: Boolean by mutableStateOf(false)
        private set

    /** Ведёт карту к своему месту, если его удаётся узнать. */
    suspend fun locate(state: MapState) {
        busy = true
        val place = locateSelf()
        asking = place == SelfPlace.AskOwner
        show(place, state)
        busy = false
    }

    /** Владелец разрешил запасной путь: решение помнится, и место ищется сразу. */
    suspend fun allow(state: MapState) {
        asking = false
        show(answerAsk(allowed = true), state)
    }

    /** Владелец отказал: наружу ничего не уходит; спросят снова только по его нажатию. */
    suspend fun deny() {
        asking = false
        answerAsk(allowed = false)
    }

    private fun show(place: SelfPlace, state: MapState) {
        if (place !is SelfPlace.Found) return
        val zoom = if (place.precise) HOUSE_ZOOM else CITY_ZOOM
        state.showLocation(place.place.latitude, place.place.longitude, place.place.title, zoom, place.precise)
    }
}
