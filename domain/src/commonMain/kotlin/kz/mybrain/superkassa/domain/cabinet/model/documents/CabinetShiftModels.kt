package kz.mybrain.superkassa.domain.cabinet.model.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal

/**
 * Смена в кабинете и её итоги.
 *
 * Отдельный предмет от документов смены: смена не фискальный документ,
 * пакета протокола у неё нет, а есть выручка, наличные и счёт чеков —
 * то, чего нет ни у одного документа.
 */

/** Итоги смены. */
data class ShiftTotals(
    val revenue: Decimal? = null,
    val cashSum: Decimal? = null,
    /**
     * Чеков в смене — всех четырёх видов.
     *
     * Кабинет отдаёт одно число на смену и не делит его по операциям.
     * Прежде оно подписывалось «Продажи», а рядом стояло «Возвраты · 0»
     * при ненулевой сумме возвратов: число было не тем, чем названо.
     */
    val receiptsCount: Int = 0,
    val salesSum: Decimal? = null,
    val returnsSum: Decimal? = null,
    val purchasesSum: Decimal? = null,
    val purchaseReturnsSum: Decimal? = null
)

/**
 * Смена. Кабинет отдаёт итоги плоскими полями; экранам они собраны
 * в [ShiftTotals], чтобы строка смены и карточка смены читали одно.
 */
data class CabinetShift(
    val shiftNumber: Int,
    val state: String? = null,
    val openedAt: String? = null,
    val closedAt: String? = null,
    val receiptsCount: Int? = null,
    val saleTotal: Decimal? = null,
    val returnTotal: Decimal? = null,
    /** Покупка у населения: касса по ней платит, и в выручку она не входит. */
    val buyTotal: Decimal? = null,
    val buyReturnTotal: Decimal? = null,
    /**
     * Наличные в денежном ящике на момент отчёта — `ZXReport.cash_sum`.
     *
     * Не то же, что `cashTotal`: тот — оплаченное наличными за смену.
     * Карточка смены подписывала «Наличные в кассе» именно им, и у смены 12
     * кассы 260940000021 показывала 5 870 ₸ вместо 6 070 ₸, лежавших
     * в ящике.
     */
    val cashBalance: Decimal? = null,
    val cashTotal: Decimal? = null
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
    val total: Decimal?
        get() = saleTotal?.let { sale -> returnTotal?.let { sale - it } ?: sale }

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

/** Разность сумм без округления: вычитаемое приводится к большей из двух точностей. */
private operator fun Decimal.minus(other: Decimal): Decimal {
    val common = maxOf(scale, other.scale)
    return Decimal(scaled(common) - other.scaled(common), common)
}
