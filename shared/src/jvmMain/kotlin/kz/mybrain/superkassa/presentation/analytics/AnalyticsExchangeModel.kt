package kz.mybrain.superkassa.presentation.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.data.cabinet.ExchangeAddresses
import kz.mybrain.superkassa.data.cabinet.exchangeAddresses
import kz.mybrain.superkassa.presentation.session.CabinetSession

/**
 * Состояние списка адресов обмена.
 *
 * Список спрашивается целиком по компании: отбор по кассе и поиск
 * работают уже по нему. Отдельная ручка по одной кассе у кабинета есть,
 * но ходить в неё на каждое нажатие в отборе значило бы ждать сеть там,
 * где ответ уже под рукой.
 *
 * В журнал приложения адреса не пишутся: это служебное сведение о том,
 * откуда выходят на связь машины владельца, и место ему на экране,
 * а не в файле, который уходит в поддержку.
 */
class AnalyticsExchangeModel(private val cabinet: CabinetSession) {

    var view: ExchangeAddresses? by mutableStateOf(null)
        private set

    var trouble: AnalyticsTrouble? by mutableStateOf(null)
        private set

    var loading: Boolean by mutableStateOf(false)
        private set

    /** Строка поиска по адресам и кассам. */
    var query: String by mutableStateOf("")

    /** Отбор по кассе; `null` — все кассы. */
    var register: String? by mutableStateOf(null)

    /** Спрашивает кабинет об адресах, с которых кассы выходили на связь. */
    suspend fun load() {
        val token = cabinet.token ?: return
        loading = true
        trouble = null
        askedCabinet { cabinet.client.exchangeAddresses(token) }
            .onSuccess { view = it }
            .onFailure {
                view = null
                trouble = analyticsTrouble(it)
            }
        loading = false
    }
}
