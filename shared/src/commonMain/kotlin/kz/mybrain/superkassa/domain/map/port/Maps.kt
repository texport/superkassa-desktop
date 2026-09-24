package kz.mybrain.superkassa.domain.map.port

import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.model.MapPointPlace

/**
 * Службы карты: плитки, поиск адреса, место под точкой и своё место.
 *
 * Все они чужие и все отвечают не всегда. Порт говорит об этом прямо:
 * `null` — служба не ответила, пустой список — ответила, но не нашла.
 * Прежде «нет сети» и «не нашлось» сливались в одно, и адрес, не найденный
 * из-за оборванной связи, до перезапуска числился ненайденным.
 *
 * Хранит найденное реализация: плитку, раз положенную на диск, и адрес,
 * раз найденный, заново у службы не спрашивают. Она же соблюдает правила
 * служб — называет себя и не спрашивает чаще, чем разрешено.
 */
interface Maps {

    /** Плитка карты в PNG; `null` — взять её неоткуда. */
    suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray?

    /** Места по адресу, ближайшее первым; пусто — не нашлось; `null` — служба не ответила. */
    suspend fun find(address: String): List<MapPlace>?

    /** Что за место под точкой словами службы; `null` — не узнано или служба не ответила. */
    suspend fun placeAt(latitude: Double, longitude: Double): MapPointPlace?

    /**
     * Своё место от самой машины — её службой геопозиции, с точностью
     * до дома. `null` — службы нет или владелец её не разрешил системе.
     */
    suspend fun locateMachine(): MapPlace?

    /**
     * Город по адресу подключения. Адрес уходит наружу, поэтому спрашивают
     * только с разрешения владельца — см. [MapMemory.locationAllowed].
     */
    suspend fun locateByConnection(): MapPlace?
}

/**
 * Что рабочее место помнит о картах между запусками.
 *
 * Свёрнутые карточка и легенда — выбор владельца, и повторять нажатие
 * при каждом открытии раздела он не должен. Разрешение на определение
 * места по адресу подключения — его решение: `null` — ещё не спрашивали.
 */
interface MapMemory {
    var cardCollapsed: Boolean
    var legendCollapsed: Boolean
    var locationAllowed: Boolean?
}
