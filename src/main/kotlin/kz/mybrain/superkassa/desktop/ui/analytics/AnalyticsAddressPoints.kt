package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.desktop.ui.map.MapGeocoder

/**
 * Координаты адресов торговых точек, найденные картой.
 *
 * Кабинет при источнике «адрес торговой точки» координат не даёт:
 * адресный регистр их не хранит. Значит, дом ищет приложение — тем же
 * поиском, каким оно ищет адрес при заведении точки.
 *
 * Найденное держится в памяти экрана: адрес торговой точки не меняется
 * от перерисовки к перерисовке, а спрашивать службу заново на каждую
 * из них — и медленно, и против её правил.
 *
 * **Про частоту.** Поиск открытый, отдан сообществом OpenStreetMap,
 * и его правила ограничивают частоту обращений. Поэтому адреса
 * спрашиваются по одному с паузой, а не сотней запросов в один миг:
 * у сети бывают сотни точек, и залпом их спрашивать нельзя.
 */
class AnalyticsAddressPoints(private val geocoder: MapGeocoder) : AddressLookup {

    private val answers: SnapshotStateMap<String, AddressAnswer> = mutableStateMapOf()

    /**
     * Что известно об адресе.
     *
     * Неизвестный адрес считается разыскиваемым: к нему очередь ещё
     * не дошла, и говорить о нём «не нашли» было бы неправдой.
     */
    override fun answer(address: String): AddressAnswer =
        answers[address.trim()] ?: AddressAnswer.Searching

    /** Сколько адресов уже спрошено: по этому числу экран знает, что работа идёт. */
    val known: Int get() = answers.size

    /**
     * Ищет адреса по одному, соблюдая паузу между обращениями.
     *
     * Уже спрошенное не спрашивается повторно, поэтому смена источника
     * положения и обновление списка не стоят ни одного лишнего запроса.
     */
    suspend fun resolve(addresses: List<String>) {
        addresses.forEach { address ->
            val key = address.trim()
            if (key.isBlank() || answers.containsKey(key)) return@forEach
            val place = geocoder.find(key).firstOrNull()
            answers[key] = place
                ?.let { AddressAnswer.Found(it.latitude, it.longitude) }
                ?: AddressAnswer.Missing
            delay(PAUSE_MS)
        }
    }

    private companion object {
        /** Пауза между обращениями к службе поиска: её правила — не чаще раза в секунду. */
        const val PAUSE_MS = 1100L
    }
}
