package kz.mybrain.superkassa.domain.cabinet.model.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal

/**
 * Из чего сложен фискальный документ: кассир, налог, позиция, оплата, итоги.
 *
 * Отдельный предмет, потому что эти части не принадлежат ни чеку, ни отчёту,
 * ни движению денег в одиночку: кассир стоит и в чеке, и во внесении, налог —
 * и у позиции, и у чека целиком. Положить их к чеку значит заставить
 * остальные документы ссылаться на чужой предмет.
 */

/** Кассир, оформивший документ. */
data class DocumentOperator(val code: Int? = null, val name: String? = null)

/** Налог позиции или чека. */
data class DocumentTax(
    val type: String? = null,
    val percent: Int? = null,
    val sum: Decimal? = null,
    val inTotalSum: Boolean? = null
)

/** Позиция чека. */
data class DocumentItem(
    val positionNumber: Int? = null,
    val type: String? = null,
    val name: String? = null,
    val sectionCode: String? = null,
    val quantity: Decimal? = null,
    val price: Decimal? = null,
    val sum: Decimal? = null,
    val taxPercent: Decimal? = null,
    val taxAmount: Decimal? = null,
    val measureUnitCode: String? = null,
    val barcode: String? = null
) {
    /** Налог позиции приходит процентом и суммой рядом с позицией, а не списком. */
    val taxes: List<DocumentTax>
        get() = if (taxAmount == null && taxPercent == null) {
            emptyList()
        } else {
            listOf(DocumentTax(type = "VAT", percent = taxPercent?.scaled(0)?.toInt(), sum = taxAmount))
        }
}

/** Оплата чека. */
data class DocumentPayment(val type: String? = null, val sum: Decimal? = null)

/** Итоги чека. */
data class DocumentAmounts(
    val total: Decimal? = null,
    val taken: Decimal? = null,
    val change: Decimal? = null,
    val discount: Decimal? = null,
    val markup: Decimal? = null
)
