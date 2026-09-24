package kz.mybrain.superkassa.domain.analytics.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/**
 * Срок торговой сводки: сутки с первых по последние включительно.
 *
 * Кабинет считает сводку не длиннее [SALES_MOST_DAYS] суток и на более
 * долгий срок отвечает отказом `ANALYTICS_PERIOD_TOO_LONG`. Поэтому срок
 * сводки строится здесь, одним правилом на раздел и на окно кассы,
 * а не берётся как есть из полосы выбора срока.
 */
data class SalesSpan(val from: LocalDate, val to: LocalDate) {

    /** Суток в сроке. */
    val days: Int get() = from.daysUntil(to) + 1

    /**
     * Можно ли сравнивать срок с прошлым.
     *
     * Срок, который кончается сегодня или позже, ещё идёт: сегодняшний
     * день неполон, и «к прошлому сроку» днём всегда показывало падение —
     * половина сегодняшнего дня против целого вчерашнего. Сравнивается
     * только закончившийся срок.
     */
    fun finished(today: LocalDate): Boolean = to < today

    /** Тот же срок перед этим: столько же суток, вплотную. */
    fun previous(): SalesSpan = SalesSpan(from.minus(DatePeriod(days = days)), to.minus(DatePeriod(days = days)))

    /** Отбор кабинета по этому сроку, по всей сети или по одной кассе. */
    fun filter(register: String? = null): SalesFilter =
        SalesFilter(from = from.toString(), to = to.toString(), cashRegisterId = register)

    companion object {

        /**
         * Срок сводки по выбранным границам.
         *
         * Границ нет — «всё время» — берутся последние [SALES_MOST_DAYS]
         * суток до сегодня: год прежде уходил кабинету и всегда получал
         * отказ. Срок длиннее предела укорачивается до последних суток
         * выбранного: свежее важнее давнего.
         */
        fun of(from: LocalDate?, to: LocalDate?, today: LocalDate): SalesSpan {
            val last = to ?: today
            val longest = last.minus(DatePeriod(days = SALES_MOST_DAYS - 1))
            val first = from?.takeIf { it >= longest } ?: longest
            return SalesSpan(first, last)
        }
    }
}

/** Сколько суток кабинет соглашается сводить за раз. */
const val SALES_MOST_DAYS: Int = 92
