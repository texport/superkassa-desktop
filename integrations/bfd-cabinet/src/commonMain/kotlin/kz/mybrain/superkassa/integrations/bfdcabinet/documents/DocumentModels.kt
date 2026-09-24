package kz.mybrain.superkassa.integrations.bfdcabinet.documents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal

/**
 * Границы периода, за который кабинет отдаёт документы.
 *
 * Кабинет отбирает по моменту приёма документа сервером и принимает обе
 * границы включительно. Момент — строкой ISO-8601 в UTC: так его ждёт
 * кабинет, и так он не зависит от часового пояса рабочего места.
 *
 * @property from начало; `null` — без ограничения.
 * @property to конец; `null` — без ограничения.
 */
data class DocumentPeriod(val from: String? = null, val to: String? = null)

/**
 * Пакет протокола, приложенный к документу: запрос кассы и ответ ОФД на него.
 *
 * Кабинет хранит пакет одним полем `jsonb` и отдаёт его то объектом,
 * то строкой с тем же объектом внутри: [packet] снимает эту разницу.
 */
interface ProtocolDocument {

    /** Пакет протокола, как его отдал кабинет. */
    val payload: JsonElement?

    /** Пакет текстом; `null` — кабинет его не отдал. */
    val packet: String?
        get() = when (val stored = payload) {
            null -> null
            is JsonPrimitive -> stored.content.takeIf { stored.isString && it.isNotBlank() }
            else -> stored.toString()
        }
}

/** Сводка регистрационной карты в обзоре документов. */
@Serializable
data class RegistrationCardSummary(
    val available: Boolean = false,
    val status: String? = null,
    val pdfStatus: String? = null,
    val updatedAt: String? = null
)

/** Сколько чего накопила касса — за всё время. */
@Serializable
data class DocumentsOverview(
    val cashRegisterId: String,
    val registrationCard: RegistrationCardSummary? = null,
    val receiptsCount: Long = 0,
    val reportsCount: Long = 0,
    val shiftsCount: Long = 0,
    val cashMovementsCount: Long = 0
)

/** Кассир, оформивший документ. */
@Serializable
data class DocumentOperator(val code: Int? = null, val name: String? = null)

/**
 * Позиция чека.
 *
 * @property sum сумма позиции (`amount` у кабинета).
 * @property taxPercent ставка НДС позиции.
 * @property taxAmount сумма НДС позиции.
 */
@Serializable
data class DocumentItem(
    val positionNumber: Int? = null,
    val type: String? = null,
    val name: String? = null,
    val sectionCode: String? = null,
    val quantity: CabinetDecimal? = null,
    val price: CabinetDecimal? = null,
    @SerialName("amount") val sum: CabinetDecimal? = null,
    val taxPercent: CabinetDecimal? = null,
    val taxAmount: CabinetDecimal? = null,
    val measureUnitCode: String? = null,
    val barcode: String? = null
)

/**
 * Смена кассы. Итоги кабинет отдаёт плоскими полями.
 *
 * @property state состояние смены (`status`).
 * @property buyTotal покупка у населения: касса по ней платит.
 * @property cashBalance наличные в денежном ящике на момент отчёта — не то же,
 *   что [cashTotal], оплаченное наличными за смену.
 */
@Serializable
data class CabinetShift(
    val shiftNumber: Int,
    @SerialName("status") val state: String? = null,
    val openedAt: String? = null,
    val closedAt: String? = null,
    val receiptsCount: Int? = null,
    val saleTotal: CabinetDecimal? = null,
    val returnTotal: CabinetDecimal? = null,
    val buyTotal: CabinetDecimal? = null,
    val buyReturnTotal: CabinetDecimal? = null,
    val cashBalance: CabinetDecimal? = null,
    val cashTotal: CabinetDecimal? = null
)
