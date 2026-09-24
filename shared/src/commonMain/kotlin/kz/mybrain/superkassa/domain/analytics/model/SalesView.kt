package kz.mybrain.superkassa.domain.analytics.model

/**
 * Вся сводка за срок одним ответом.
 *
 * Границы срока лежат здесь же: ряд столбиков по дням достраивает пустые
 * сутки, а знать о них без границ неоткуда — кабинет присылает только
 * те сутки, в которые торговали.
 *
 * @param previous итоги прошлого срока такой же длины; `null` — сравнивать
 *   не с чем: кабинет о нём не ответил или срок ещё идёт.
 * @param running срок кончается сегодня или позже: сегодняшний день
 *   неполон, и сравнение с прошлым сроком не делается вовсе — см.
 *   [SalesSpan.finished].
 * @param retailPlaces торговые точки компании. Нужны своду по регионам:
 *   регион стоит в адресе точки, а в строках сводки адреса нет.
 */
data class SalesView(
    val range: SalesSpan,
    val summary: SalesSummary,
    val days: List<SalesDay>,
    val hours: List<SalesHour>,
    val registers: List<SalesUnit>,
    val places: List<SalesUnit>,
    val delivery: SalesDelivery,
    val previous: SalesSummary? = null,
    val retailPlaces: List<PlaceAddress> = emptyList(),
    val running: Boolean = false
) {
    /**
     * Ни одного чека за срок.
     *
     * Считается по числам, а не по длине списков: кабинет присылает
     * только непустые сутки, и месяц без единого документа приходит
     * пустым списком — как и месяц, ответ по которому ещё не разобран.
     *
     * Покупка у населения считается наравне с продажей: срок, в который
     * касса только скупала, документы за собой оставил, и надпись
     * «документов нет» над ними была бы неправдой.
     */
    val empty: Boolean
        get() = summary.receiptCount == 0 &&
            !summary.purchased &&
            days.none { it.receiptCount > 0 } &&
            registers.none { it.receiptCount > 0 }

    companion object {
        /** Разрезы кабинета со сроком и тем, что к ним прочитано рядом. */
        fun of(span: SalesSpan, figures: SalesFigures): SalesView = SalesView(
            range = span,
            summary = figures.summary,
            days = figures.days,
            hours = figures.hours,
            registers = figures.registers,
            places = figures.places,
            delivery = figures.delivery
        )
    }
}
