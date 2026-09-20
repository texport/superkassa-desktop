package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Ответы аналитики кабинета: кассы на карте и адреса, с которых они
 * выходят на связь.
 *
 * Время приходит строкой ISO-8601 и хранится строкой — как и везде
 * в кабинете: оно нужно для показа, а не для расчёта.
 */

/**
 * Откуда брать положение кассы.
 *
 * Три источника отвечают на три разных вопроса. Адрес торговой точки —
 * где касса должна стоять по учётным сведениям КГД. Координаты кабинета —
 * где владелец поставил точку сам, когда заводил торговую точку.
 * Координаты кассы — где машина считает себя находящейся прямо сейчас.
 * Расхождение между ними и есть то, ради чего владелец открывает карту.
 */
enum class PositionSource(val code: String) {
    RetailPlaceAddress("RETAIL_PLACE_ADDRESS"),
    CabinetCoordinates("CABINET_COORDINATES"),
    KkmCoordinates("KKM_COORDINATES");

    companion object {
        fun byCode(code: String?): PositionSource =
            entries.firstOrNull { it.code == code } ?: RetailPlaceAddress
    }
}

/**
 * Положение кассы так, как его отдаёт кабинет.
 *
 * При источнике «адрес торговой точки» координат здесь нет вовсе:
 * адресный регистр их не хранит, а геокодера в кабинете нет. Приходят
 * текст адреса, РКА и САТО, а точку на карте ставит приложение — оно
 * умеет искать адрес в карте, кабинет не умеет.
 */
@Serializable
data class KkmPosition(
    val source: String? = null,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null,
    /** Чем определены координаты кассы: приёмник, сеть, заданы вручную. */
    val geoSource: String? = null,
    val rka: String? = null,
    val cato: String? = null
) {
    /** Есть ли готовая точка. Нет — искать по адресу. */
    val degrees: Boolean get() = latitude != null && longitude != null
}

/** Касса в аналитике: паспорт, состояние и положение одной строкой. */
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
 * и те, кого не может. Второй список не мусор, а работа владельца:
 * у кассы не выбран адрес, не заданы координаты или машина ни разу
 * не сообщила о себе.
 */
@Serializable
data class KkmMapView(
    val positionSource: String? = null,
    val placedCount: Int = 0,
    val withoutPositionCount: Int = 0,
    val placed: List<AnalyticsKkm> = emptyList(),
    val withoutPosition: List<AnalyticsKkm> = emptyList()
) {
    val source: PositionSource get() = PositionSource.byCode(positionSource)
}

/**
 * Адрес, с которого касса присылала данные.
 *
 * Сведение служебное: по нему видно, что касса, числящаяся в Актобе,
 * выходит на связь из другого города, и что под одним адресом работает
 * десяток машин. Владельцу оно показывается, в журнал приложения
 * не пишется.
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
 * У кассы без единого обмена список пуст, а счётчики нулевые: кабинет
 * отвечает пустотой, а не отсутствием ручки.
 */
@Serializable
data class ExchangeAddresses(
    val cashRegisterCount: Int = 0,
    val addressCount: Int = 0,
    val addresses: List<ExchangeAddress> = emptyList()
)
