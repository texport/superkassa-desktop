package kz.mybrain.superkassa.integrations.bfdcabinet.analytics

import kotlinx.serialization.Serializable

/**
 * Откуда брать положение кассы на карте.
 *
 * Три источника отвечают на три вопроса: где касса должна стоять по учёту
 * КГД, где владелец поставил точку сам и где машина считает себя сейчас.
 *
 * @property code как источник называет кабинет.
 */
enum class PositionSource(val code: String) {
    /** Адрес торговой точки: координат нет, есть адрес, РКА и КАТО. */
    RetailPlaceAddress("RETAIL_PLACE_ADDRESS"),

    /** Координаты, заданные владельцем в кабинете. */
    CabinetCoordinates("CABINET_COORDINATES"),

    /** Координаты, присланные кассой. */
    KkmCoordinates("KKM_COORDINATES");

    /** Источник по коду кабинета. */
    companion object {
        /** Источник по коду; незнакомый код — адрес торговой точки. */
        fun byCode(code: String?): PositionSource = entries.firstOrNull { it.code == code } ?: RetailPlaceAddress
    }
}

/**
 * Положение кассы, как его отдаёт кабинет.
 *
 * При источнике «адрес торговой точки» координат нет: регистр их не хранит,
 * а геокодера у кабинета нет. Точку по адресу ставит приложение.
 *
 * @property geoSource чем определены координаты кассы: приёмник, сеть, вручную.
 */
@Serializable
data class KkmPosition(
    val source: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geoSource: String? = null,
    val rka: String? = null,
    val cato: String? = null
)

/** Касса в аналитике: паспорт, учёт, смена и положение одной строкой. */
@Serializable
data class AnalyticsKkm(
    val cashRegisterId: String,
    val kkmId: Int = 0,
    val registrationNumber: String? = null,
    val internalName: String? = null,
    val retailPlaceId: String? = null,
    val retailPlaceName: String? = null,
    val address: String? = null,
    val status: String? = null,
    val blocked: Boolean = false,
    val shiftStatus: String? = null,
    val shiftNumber: Long? = null,
    val lastContactAt: String? = null,
    val position: KkmPosition? = null
)

/**
 * Кассы компании на карте.
 *
 * Список разделён самим кабинетом: те, кого он может поставить на карту,
 * и те, кого не может, — у них не выбран адрес или машина не сообщала о себе.
 */
@Serializable
data class KkmMapView(
    val positionSource: String? = null,
    val placedCount: Int = 0,
    val withoutPositionCount: Int = 0,
    val placed: List<AnalyticsKkm> = emptyList(),
    val withoutPosition: List<AnalyticsKkm> = emptyList()
) {
    /** Источник положения, по которому кабинет расставил кассы. */
    val source: PositionSource get() = PositionSource.byCode(positionSource)
}

/**
 * Адрес, с которого касса присылала данные.
 *
 * @property address сетевой адрес обмена.
 * @property firstSeen первый обмен с этого адреса.
 * @property lastSeen последний обмен с этого адреса.
 */
@Serializable
data class ExchangeAddress(
    val cashRegisterId: String,
    val kkmId: Int = 0,
    val registrationNumber: String? = null,
    val internalName: String? = null,
    val retailPlaceName: String? = null,
    val address: String = "",
    val firstSeen: String? = null,
    val lastSeen: String? = null
)

/**
 * Адреса обмена: по всем кассам компании или по одной.
 *
 * У кассы без единого обмена список пуст, а счётчики нулевые — не отказ.
 * [addressCount] кабинет считает записями «касса и адрес», а не разными адресами.
 */
@Serializable
data class ExchangeAddresses(
    val cashRegisterCount: Int = 0,
    val addressCount: Int = 0,
    val addresses: List<ExchangeAddress> = emptyList()
)
