package kz.mybrain.superkassa.domain.analytics.model

import kz.mybrain.superkassa.domain.cabinet.model.KkmRecord
import kz.mybrain.superkassa.domain.cabinet.model.kkmRecord

/**
 * Кассы компании глазами аналитики кабинета: паспорт, учёт, смена
 * и положение одной строкой.
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
data class KkmPosition(
    val source: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Чем определены координаты кассы: приёмник, сеть, заданы вручную. */
    val geoSource: String? = null,
    val rka: String? = null,
    val cato: String? = null
) {
    /** Есть ли готовая точка. Нет — искать по адресу. */
    val degrees: Boolean get() = latitude != null && longitude != null
}

/** Касса в аналитике: паспорт, состояние и положение одной строкой. */
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
) {
    /** Что КГД знает о кассе — смысл кода учёта, а не сам код. */
    val record: KkmRecord get() = kkmRecord(status)

    /**
     * Открыта ли смена.
     *
     * Единственное место, где спрашивается смена кассы: учёт, карта
     * и окно кассы считают открытые смены по этому признаку, а не каждый
     * своим числом.
     */
    val shiftOpen: Boolean get() = shiftStatus?.trim()?.uppercase() == SHIFT_OPEN

    /**
     * Как кассу зовут в списках.
     *
     * Своё название владельца, а не номер КГД: номер он видит в заявлении
     * раз в жизни кассы. Названия нет — номер КГД, а у кассы, ещё
     * не поставленной на учёт, нет и его: тогда номер самой машины.
     */
    val title: String get() = kkmTitle(internalName, registrationNumber, kkmId)
}

/**
 * Кассы компании на карте.
 *
 * Список разделён самим кабинетом: те, кого он может поставить на карту,
 * и те, кого не может. Второй список не мусор, а работа владельца:
 * у кассы не выбран адрес, не заданы координаты или машина ни разу
 * не сообщила о себе.
 */
data class KkmMapView(
    val positionSource: String? = null,
    val placedCount: Int = 0,
    val withoutPositionCount: Int = 0,
    val placed: List<AnalyticsKkm> = emptyList(),
    val withoutPosition: List<AnalyticsKkm> = emptyList()
) {
    val source: PositionSource get() = PositionSource.byCode(positionSource)

    /** Все кассы компании: и поставленные на карту, и те, которых поставить некуда. */
    val kkms: List<AnalyticsKkm> get() = placed + withoutPosition
}

/** Одно правило названия кассы на все списки аналитики. */
internal fun kkmTitle(internalName: String?, registrationNumber: String?, kkmId: Int): String =
    internalName?.takeIf { it.isNotBlank() }
        ?: registrationNumber?.takeIf { it.isNotBlank() }
        ?: "№ $kkmId"

/** Код открытой смены у кабинета. */
private const val SHIFT_OPEN = "OPEN"
