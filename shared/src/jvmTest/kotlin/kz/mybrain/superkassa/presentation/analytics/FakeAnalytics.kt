package kz.mybrain.superkassa.presentation.analytics

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddresses
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesFilter
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.map.MemoryMapMemory
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.domain.map.port.Maps
import kz.mybrain.superkassa.presentation.analytics.map.AnalyticsMapViewModel
import kz.mybrain.superkassa.presentation.analytics.map.KkmMapCases
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Аналитика кабинета по заказу проверки.
 *
 * Не заказанное отвечает «раздел не выложен» — как кабинет, в котором
 * ручки ещё нет. Каждое обращение записывается: проверки спрашивают,
 * что и с каким отбором модель спросила у кабинета.
 */
class FakeAnalytics : Analytics {
    var kkms: (PositionSource) -> AnalyticsAnswer<KkmMapView> = { absent() }
    var exchange: () -> AnalyticsAnswer<ExchangeAddresses> = { absent() }
    var sales: (SalesFilter) -> AnalyticsAnswer<SalesFigures> = { absent() }
    var summary: (SalesFilter) -> AnalyticsAnswer<SalesSummary> = { absent() }
    var places: () -> AnalyticsAnswer<List<PlaceAddress>> = { absent() }

    /** Обращения по порядку: что спрошено и с каким отбором. */
    val asked = mutableListOf<String>()

    override suspend fun kkms(source: PositionSource) = kkms.invoke(source).also { asked += "kkms $source" }

    override suspend fun exchange() = exchange.invoke().also { asked += "exchange" }

    override suspend fun sales(filter: SalesFilter) = sales.invoke(filter).also { asked += "sales $filter" }

    override suspend fun summary(filter: SalesFilter) = summary.invoke(filter).also { asked += "summary $filter" }

    override suspend fun places() = places.invoke().also { asked += "places" }

    private fun absent() = AnalyticsAnswer.Troubled(AnalyticsTrouble.NotDeployed)
}

/** Порты аналитики для проверок: кабинет по заказу, карта без сети, память в процессе. */
fun analyticsPorts(cabinet: Analytics = FakeAnalytics(), maps: Maps = QuietMaps()) =
    AnalyticsPorts(cabinet, MapPorts(maps, MemoryMapMemory()))

/** Те же зависимости окна с аналитикой, которая ходит в [cabinet]. */
fun AppContainer.analyzing(cabinet: Analytics): AppContainer =
    AppContainer(kassa, signIn, memory, look, talk, areas.copy(analytics = analyticsPorts(cabinet)))

/** Модель карты касс на подставном кабинете и службах карт; память карты — своя у каждой проверки. */
fun mapModel(cabinet: Analytics, maps: Maps = QuietMaps()): AnalyticsMapViewModel =
    AnalyticsMapViewModel(KkmMapCases(cabinet, MapPorts(maps, MemoryMapMemory()).cases()))
