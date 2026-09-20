package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Из чего сложен фискальный документ: кассир, налог, позиция, оплата, итоги.
 *
 * Отдельный предмет, потому что эти части не принадлежат ни чеку, ни отчёту,
 * ни движению денег в одиночку: кассир стоит и в чеке, и во внесении, налог —
 * и у позиции, и у чека целиком. Положить их к чеку значит заставить
 * остальные документы ссылаться на чужой предмет.
 */

/** Кассир, оформивший документ. */
@Serializable
data class DocumentOperator(val code: Int? = null, val name: String? = null)

/** Налог позиции или чека. */
@Serializable
data class DocumentTax(
    val type: String? = null,
    val percent: Int? = null,
    @Contextual val sum: BigDecimal? = null,
    val inTotalSum: Boolean? = null
)

/** Позиция чека. */
@Serializable
data class DocumentItem(
    val positionNumber: Int? = null,
    val type: String? = null,
    val name: String? = null,
    val sectionCode: String? = null,
    @Contextual val quantity: BigDecimal? = null,
    @Contextual val price: BigDecimal? = null,
    @SerialName("amount") @Contextual val sum: BigDecimal? = null,
    @Contextual val taxPercent: BigDecimal? = null,
    @Contextual val taxAmount: BigDecimal? = null,
    val measureUnitCode: String? = null,
    val barcode: String? = null
) {
    /** Налог позиции приходит процентом и суммой рядом с позицией, а не списком. */
    val taxes: List<DocumentTax>
        get() = if (taxAmount == null && taxPercent == null) {
            emptyList()
        } else {
            listOf(DocumentTax(type = "VAT", percent = taxPercent?.toInt(), sum = taxAmount))
        }
}

/** Оплата чека. */
@Serializable
data class DocumentPayment(val type: String? = null, @Contextual val sum: BigDecimal? = null)

/** Итоги чека. */
@Serializable
data class DocumentAmounts(
    @Contextual val total: BigDecimal? = null,
    @Contextual val taken: BigDecimal? = null,
    @Contextual val change: BigDecimal? = null,
    @Contextual val discount: BigDecimal? = null,
    @Contextual val markup: BigDecimal? = null
)
