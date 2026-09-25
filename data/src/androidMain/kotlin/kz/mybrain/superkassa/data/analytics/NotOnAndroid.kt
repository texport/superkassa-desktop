package kz.mybrain.superkassa.data.analytics

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
import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.model.MapPointPlace
import kz.mybrain.superkassa.domain.map.port.Maps

/**
 * Аналитика и карты на Android — пока их нет.
 *
 * Аналитика — раздел кабинета, а в кабинет входят подписью ЭЦП, которой
 * на Android пока нет; экранов карты здесь тоже нет. Порты собраны, чтобы
 * контейнер был один на обе платформы: на вопрос аналитика отвечает
 * «раздел не выложен», а карты не отдают ничего — так же, как без сети.
 */
class AnalyticsNotOnAndroid : Analytics {
    override suspend fun kkms(source: PositionSource): AnalyticsAnswer<KkmMapView> = absent()
    override suspend fun exchange(): AnalyticsAnswer<ExchangeAddresses> = absent()
    override suspend fun sales(filter: SalesFilter): AnalyticsAnswer<SalesFigures> = absent()
    override suspend fun summary(filter: SalesFilter): AnalyticsAnswer<SalesSummary> = absent()
    override suspend fun places(): AnalyticsAnswer<List<PlaceAddress>> = absent()

    private fun absent() = AnalyticsAnswer.Troubled(AnalyticsTrouble.NotDeployed)
}

/** Карты на Android: ни плиток, ни поиска — как без сети. */
class MapsNotOnAndroid : Maps {
    override suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray? = null
    override suspend fun find(address: String): List<MapPlace>? = null
    override suspend fun placeAt(latitude: Double, longitude: Double): MapPointPlace? = null
    override suspend fun locateMachine(): MapPlace? = null
    override suspend fun locateByConnection(): MapPlace? = null
}
