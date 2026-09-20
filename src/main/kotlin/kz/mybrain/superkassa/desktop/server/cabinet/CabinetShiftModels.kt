package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Смена в кабинете и её итоги.
 *
 * Отдельный предмет от документов смены: смена не фискальный документ,
 * пакета протокола у неё нет, а есть выручка, наличные и счёт чеков —
 * то, чего нет ни у одного документа.
 */

/** Итоги смены. */
@Serializable
data class ShiftTotals(
    @Contextual val revenue: BigDecimal? = null,
    @Contextual val cashSum: BigDecimal? = null,
    /**
     * Чеков в смене — всех четырёх видов.
     *
     * Кабинет отдаёт одно число на смену и не делит его по операциям.
     * Прежде оно подписывалось «Продажи», а рядом стояло «Возвраты · 0»
     * при ненулевой сумме возвратов: число было не тем, чем названо.
     */
    val receiptsCount: Int = 0,
    @Contextual val salesSum: BigDecimal? = null,
    @Contextual val returnsSum: BigDecimal? = null,
    @Contextual val purchasesSum: BigDecimal? = null,
    @Contextual val purchaseReturnsSum: BigDecimal? = null
)

/**
 * Смена. Кабинет отдаёт итоги плоскими полями; экранам они собраны
 * в [ShiftTotals], чтобы строка смены и карточка смены читали одно.
 */
@Serializable
data class CabinetShift(
    val shiftNumber: Int,
    @SerialName("status") val state: String? = null,
    val openedAt: String? = null,
    val closedAt: String? = null,
    val receiptsCount: Int? = null,
    @Contextual val saleTotal: BigDecimal? = null,
    @Contextual val returnTotal: BigDecimal? = null,
    /** Покупка у населения: касса по ней платит, и в выручку она не входит. */
    @Contextual val buyTotal: BigDecimal? = null,
    @Contextual val buyReturnTotal: BigDecimal? = null,
    /**
     * Наличные в денежном ящике на момент отчёта — `ZXReport.cash_sum`.
     *
     * Не то же, что `cashTotal`: тот — оплаченное наличными за смену.
     * Карточка смены подписывала «Наличные в кассе» именно им, и у смены 12
     * кассы 260940000021 показывала 5 870 ₸ вместо 6 070 ₸, лежавших
     * в ящике.
     */
    @Contextual val cashBalance: BigDecimal? = null,
    @Contextual val cashTotal: BigDecimal? = null
) {
    /**
     * Выручка смены: продажи за вычетом возвратов.
     *
     * Прежде и строка журнала, и подпись «Выручка» в карточке брали
     * продажи как есть. У смены с продажами 3 570 ₸ и возвратами 1 900 ₸
     * выручка стояла 3 570 ₸ — рядом с самими возвратами строкой ниже.
     *
     * Покупка у населения сюда не входит: по ней касса платит, а не
     * получает, и в выручке ей места нет — она стоит своей строкой.
     */
    val total: BigDecimal?
        get() = saleTotal?.let { sale -> returnTotal?.let(sale::subtract) ?: sale }

    val totals: ShiftTotals?
        get() = if (receiptsCount == null && saleTotal == null && cashBalance == null) {
            null
        } else {
            ShiftTotals(
                revenue = total,
                cashSum = cashBalance,
                receiptsCount = receiptsCount ?: 0,
                salesSum = saleTotal,
                returnsSum = returnTotal,
                purchasesSum = buyTotal,
                purchaseReturnsSum = buyReturnTotal
            )
        }
}
