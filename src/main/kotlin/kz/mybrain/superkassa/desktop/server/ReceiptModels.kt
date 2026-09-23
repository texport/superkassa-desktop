package kz.mybrain.superkassa.desktop.server

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** Позиция чека. */
@Serializable
data class ReceiptItem(
    val name: String,
    @Contextual
    val price: BigDecimal,
    @Contextual
    val quantity: BigDecimal,
    val vatGroup: String? = null,
    @Contextual
    val discountSum: BigDecimal? = null,
    @Contextual
    val markupSum: BigDecimal? = null,
    val sectionCode: String? = null,
    val measureUnitCode: String? = null,
    val isStorno: Boolean? = null,
    /** Наименование на казахском: печатается на чеке, в ОФД не уходит. */
    val nameKk: String? = null,
    /** НТИН товара из справочника номенклатуры. */
    val ntin: String? = null,
    /**
     * Акцизные марки позиции.
     *
     * Марка на бутылке и пачке — учётный документ КГД: без неё подакцизный
     * товар в чеке считается непрослеженным. Марок бывает столько же,
     * сколько единиц товара в позиции, поэтому это перечень, а не строка.
     */
    val listExciseStamp: List<String>? = null
)

/** Оплата по чеку. */
@Serializable
data class ReceiptPayment(
    val type: String,
    @Contextual
    val sum: BigDecimal
)

/** Чек-основание для возврата. */
@Serializable
data class ParentTicket(
    val parentTicketNumber: Long,
    val parentTicketDateTime: String,
    val kgdKkmId: String,
    @Contextual
    val parentTicketTotal: BigDecimal,
    val parentTicketIsOffline: Boolean
)

/** Запрос на оформление чека. */
@Serializable
data class ReceiptRequest(
    val idempotencyKey: String,
    val items: List<ReceiptItem>,
    val payments: List<ReceiptPayment>,
    @Contextual
    val discountSum: BigDecimal? = null,
    @Contextual
    val markupSum: BigDecimal? = null,
    @Contextual
    val taken: BigDecimal? = null,
    val customerBin: String? = null,
    val parentTicket: ParentTicket? = null,
    val domain: ReceiptDomain = ReceiptDomain(TRADING_DOMAIN),
    /**
     * Ставка НДС чека по умолчанию.
     *
     * Ею узел облагает позиции, у которых своей ставки нет. Касса берёт
     * её из настроек самой кассы: у плательщика НДС чек без этого поля
     * опирался бы на умолчание узла, а не на режим кассы.
     */
    val defaultVatGroup: String? = null
)

/**
 * Отраслевые реквизиты чека.
 *
 * Вид отрасли определяет, какой подблок обязателен: услуги требуют номер
 * счёта, такси — номер машины, стоянка — время въезда и выезда. Присылать
 * два подблока разом нельзя: ОФД такой чек отвергнет.
 */
@Serializable
data class ReceiptDomain(
    val type: String,
    val services: DomainServices? = null,
    val gasOil: DomainGasOil? = null,
    val taxi: DomainTaxi? = null,
    val parking: DomainParking? = null
)

@Serializable
data class DomainServices(val accountNumber: String)

@Serializable
data class DomainGasOil(
    val cardNumber: String? = null,
    val correctionNumber: String? = null,
    @Contextual
    val correctionSum: BigDecimal? = null
)

@Serializable
data class DomainTaxi(
    val carNumber: String,
    val isOrder: Boolean,
    @Contextual
    val currentFee: BigDecimal
)

@Serializable
data class DomainParking(
    val beginTimeMillis: Long,
    val endTimeMillis: Long
)

/** Торговля: вид отрасли обычного чека. */
const val TRADING_DOMAIN: String = "DOMAIN_TRADING"

/** Запрос на внесение или изъятие наличных. */
@Serializable
data class CashRequest(
    @Contextual
    val amount: BigDecimal,
    val idempotencyKey: String
)

/** Ставки НДС, которые узел разрешает в чеке. */
@Serializable
data class VatRate(
    val code: String? = null,
    val name: String? = null,
    @Contextual
    val percent: BigDecimal? = null
)
