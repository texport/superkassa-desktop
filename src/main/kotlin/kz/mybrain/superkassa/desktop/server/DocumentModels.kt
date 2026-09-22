package kz.mybrain.superkassa.desktop.server

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Фискальный документ из журнала кассы.
 *
 * Узел называет идентификатор `id` в журнале и `documentId` в ответе на чек —
 * приложение принимает оба написания, чтобы один и тот же документ не выглядел
 * двумя разными.
 */
@Serializable
data class Document(
    val id: String,
    val docNo: Long? = null,
    /**
     * Номер, которым чек назван на бумаге.
     *
     * Касса нумерует документы сама, и этот номер стоит на чеке
     * покупателя. Номер `docNo` присваивает ОФД: он не последователен,
     * а у отвергнутого чека его нет вовсе — сверять бумагу с экраном
     * по нему кассир не может.
     */
    val printedDocumentNumber: Long? = null,
    val docType: String? = null,
    val ofdStatus: String? = null,
    val ofdErrorCode: Int? = null,
    /** Причина отказа словами ОФД: по одному коду её не найти. */
    val ofdErrorText: String? = null,
    val fiscalSign: String? = null,
    val autonomousSign: String? = null,
    val isAutonomous: Boolean? = null,
    val totalAmount: Long? = null,
    val createdAt: Long? = null,
    val deliveredAt: Long? = null,
    val shiftNo: Int? = null,
    val receiptUrl: String? = null
) {

    /**
     * Номер, которым документ назван кассиру и покупателю.
     *
     * Один на все экраны и на печатную форму: номер от ОФД не совпадает
     * с бумажным, и разные экраны называли один документ разными
     * числами.
     */
    val number: Long?
        get() = printedDocumentNumber ?: docNo

    /**
     * Можно ли показать и напечатать печатную форму документа.
     *
     * Отклонённый ОФД документ фискальным чеком не является: его печатная
     * форма выглядит как настоящий чек и вводила бы покупателя в
     * заблуждение. Автономный чек печатается — он фискальный, просто
     * ещё не доставлен; отказ ОФД — другое дело.
     */
    val printable: Boolean
        get() = !refusedByOfd

    /**
     * ОФД документ не принял.
     *
     * Фискальным чеком он не стал: его нет ни в ОФД, ни в отчётности,
     * и опереться на него нельзя ни печатной формой, ни возвратом.
     * Автономный чек сюда не попадает — он фискальный, просто ещё
     * не доставлен.
     */
    val refusedByOfd: Boolean
        get() = ofdErrorCode != null || ofdStatus == REFUSED

    /**
     * Код отказа, который есть смысл показать кассиру.
     *
     * Коды результата CPCR положительны: ноль — это приём, а `-1` узел
     * когда-то подставлял сам, когда ОФД отказал на уровне протокола
     * и своего кода не присылал. Такие значения не показываются: отказ
     * объясняют слова, а выдуманное число кассир принимает за настоящее.
     */
    val refusalCode: Int?
        get() = ofdErrorCode?.takeIf { it > 0 }
}

/** Ответ ОФД: документ отвергнут. */
private const val REFUSED = "FAILED"

/** Ответ узла на фискальную команду. */
@Serializable
data class FiscalResult(
    val documentId: String? = null,
    val deliveryStatus: String? = null,
    val deliveryError: String? = null,
    val code: String? = null,
    val message: String? = null
) {
    val isDelivered: Boolean get() = deliveryStatus == "ONLINE_OK"
    val isQueued: Boolean get() = deliveryStatus == "OFFLINE_QUEUED"
}

/**
 * Виды документов, у которых своей суммы не бывает.
 *
 * Отчёт и открытие смены суммы не несут: узел держит у них ноль, а ноль
 * в столбце «Сумма» читается как «не продано ничего». Итоги смены лежат
 * внутри самого отчёта, и подменять их нулём нельзя.
 */
private val WITHOUT_AMOUNT = setOf("SHIFT_OPEN", "SHIFT_CLOSE", "X_REPORT", "Z_REPORT")

/** Есть ли у документа своя сумма. */
val Document.hasOwnAmount: Boolean get() = docType !in WITHOUT_AMOUNT

/** Задача очереди отложенной отправки. */
@Serializable
data class QueueTask(
    val id: String,
    val type: String? = null,
    val status: String? = null,
    val attempt: Int? = null,
    val lastError: String? = null,
    val errorRu: String? = null,
    val errorKk: String? = null,
    val errorEn: String? = null,
    val nextAttemptAt: Long? = null
) {

    /**
     * Причина неудачи на языке кассира.
     *
     * Узел отдаёт причину и тремя отдельными полями, и слепленной строкой
     * «RU: … | KK: … | EN: …». Показывать слепленную нельзя: кассир читает
     * одну строку на своём языке, а не три чужих подряд.
     */
    fun reason(language: String): String? = when (language) {
        "kk" -> errorKk
        "en" -> errorEn
        else -> errorRu
    }?.takeIf { it.isNotBlank() } ?: lastError?.takeIf { it.isNotBlank() }
    /**
     * Задача ещё не доставлена.
     *
     * Отправляемая прямо сейчас (IN_PROGRESS) тоже ждёт: без неё узел
     * показывал бы её доставленной и голым кодом.
     */
    val isWaiting: Boolean
        get() = status == "PENDING" || status == "FAILED" || status == "IN_PROGRESS"
}

/**
 * Счётчик кассы.
 *
 * Узел отдаёт счётчики записями с областью действия, а не отображением:
 * один и тот же ключ существует и в целом по кассе, и в рамках смены,
 * и путать их нельзя — «наличные в кассе» за смену и за всё время разные.
 */
@Serializable
data class CounterRecord(
    val scope: String? = null,
    val key: String? = null,
    val value: Long? = null,
    val shiftId: String? = null,
    val updatedAt: Long? = null
)

/** Ошибка, пришедшая от узла с кодом и трёхъязычным текстом. */
@Serializable
data class ServerError(
    val code: String? = null,
    val message: String? = null
)

/**
 * Документ вместе с составом чека.
 *
 * @property document Сам документ.
 * @property items Позиции чека; у отчётов и операций с наличными пусто.
 * @property operatorName Кто оформил документ.
 */
@Serializable
data class DocumentDetails(
    val document: Document,
    val items: List<SoldItem> = emptyList(),
    val operatorName: String? = null
)

/**
 * Проданная позиция, как её вернуть.
 *
 * Количество приходит в тысячных долях — так его хранит протокол;
 * переводить его в дробное число здесь значило бы округлить дважды.
 */
@Serializable
data class SoldItem(
    val name: String,
    val nameKk: String? = null,
    @Contextual val price: BigDecimal,
    val quantityThousandths: Long,
    @Contextual val sum: BigDecimal,
    val vatGroup: String? = null,
    val measureUnitCode: String? = null,
    val barcode: String? = null,
    val isStorno: Boolean = false
)
