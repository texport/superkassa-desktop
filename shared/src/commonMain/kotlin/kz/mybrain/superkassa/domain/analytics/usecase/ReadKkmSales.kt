package kz.mybrain.superkassa.domain.analytics.usecase

import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.domain.analytics.port.Analytics

/**
 * Торговая сводка одной кассы: те же разрезы и то же сравнение, что у сети,
 * но с отбором по кассе. Свода по регионам у одной кассы нет, и торговые
 * точки не читаются.
 *
 * @param today какой сегодня день.
 */
class ReadKkmSales(private val analytics: Analytics, private val today: () -> LocalDate) {

    /** @param register касса, по которой сводка. */
    suspend operator fun invoke(register: String, from: LocalDate?, to: LocalDate?): AnalyticsAnswer<SalesView> {
        val now = today()
        return readSpan(analytics, SalesSpan.of(from, to, now), now, register) { emptyList() }
    }
}
