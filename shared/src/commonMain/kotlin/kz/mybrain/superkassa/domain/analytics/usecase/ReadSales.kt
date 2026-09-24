package kz.mybrain.superkassa.domain.analytics.usecase

import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.domain.analytics.model.openShifts
import kz.mybrain.superkassa.domain.analytics.model.valueOrNull
import kz.mybrain.superkassa.domain.analytics.port.Analytics

/**
 * Торговая сводка сети за срок — с прошлым сроком, сводом по регионам
 * и числом открытых смен.
 *
 * Срок строится одним правилом раздела ([SalesSpan.of]): кабинет не сводит
 * больше [kz.mybrain.superkassa.domain.analytics.model.SALES_MOST_DAYS] суток.
 * Торговые точки нужны только своду по регионам и читаются один раз
 * за вход владельца: их адреса за разговор не меняются.
 *
 * Открытые смены считаются по кассам, как в учёте ([openShifts]), а не
 * берутся из сводки кабинета: смена — свойство кассы, и число на разделе
 * одно. Смены меняются за разговор, поэтому кассы читаются каждый раз.
 *
 * @param today какой сегодня день.
 */
class ReadSales(private val analytics: Analytics, private val today: () -> LocalDate) {
    private var catalogue: List<PlaceAddress>? = null

    /**
     * @param from первые сутки выбранного срока; `null` — «всё время».
     * @param to последние сутки выбранного срока; `null` — по сегодня.
     */
    suspend operator fun invoke(from: LocalDate?, to: LocalDate?): AnalyticsAnswer<SalesView> {
        return readSpan(analytics, SalesSpan.of(from, to, today()), register = null, ::places, ::shifts)
    }

    /** Вошёл другой владелец: его точки — другие. */
    fun forget() {
        catalogue = null
    }

    private suspend fun shifts(): Int? =
        analytics.kkms(PositionSource.RetailPlaceAddress).valueOrNull()?.let { openShifts(it.kkms) }

    private suspend fun places(): List<PlaceAddress> =
        catalogue ?: analytics.places().valueOrNull()?.also { catalogue = it }.orEmpty()
}
