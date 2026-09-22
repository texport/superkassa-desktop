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
    val domain: ReceiptDomain = ReceiptDomain(),
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
 * Вид отрасли чека.
 *
 * Протокол требует его у каждого чека, и касса всегда торгует: отраслевые
 * реквизиты — лицевой счёт, номер машины, время стоянки — кассир магазина
 * не заполняет, и спрашивать их с него незачем.
 */
@Serializable
data class ReceiptDomain(val type: String = TRADING_DOMAIN)

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
