package kz.mybrain.superkassa.integrations.bfdcabinet.analytics

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.query

/**
 * Аналитика кабинета по кассам компании: где стоят, откуда выходят на связь
 * и чем торгуют. Считает кабинет — чеки и учёт лежат у него.
 *
 * Пока раздел не выложен, кабинет отвечает `404`: это `CabinetRefusal`
 * с `httpStatus = 404`, и значит оно «раздела ещё нет», а не «не найдено».
 */
class AnalyticsApi internal constructor(private val link: CabinetLink) {

    /** Кассы на карте; положение — из выбранного источника. */
    suspend fun map(source: PositionSource, retailPlaceId: String? = null, status: String? = null): KkmMapView {
        val filter = query("positionSource" to source.code, "retailPlaceId" to retailPlaceId, "status" to status)
        return link.get("$REGISTERS/map$filter")
    }

    /** Адреса, с которых выходили на связь все кассы компании. */
    suspend fun exchangeAddresses(): ExchangeAddresses = link.get("$REGISTERS/addresses")

    /** Адреса обмена одной кассы. */
    suspend fun exchangeAddresses(registerId: String): ExchangeAddresses =
        link.get("$REGISTERS/${registerId.inPath()}/addresses")

    /** Главные числа срока. */
    suspend fun summary(filter: SalesFilter): SalesSummary = link.get("$SALES/summary" + filter.query())

    /** Выручка по суткам. */
    suspend fun byDay(filter: SalesFilter): List<SalesDay> = link.rows("$SALES/by-day" + filter.query())

    /** Нагрузка по часам суток. */
    suspend fun byHour(filter: SalesFilter): List<SalesHour> = link.rows("$SALES/by-hour" + filter.query())

    /** Сводка по кассам. */
    suspend fun byRegister(filter: SalesFilter): List<SalesUnit> = link.rows("$SALES/by-cash-register" + filter.query())

    /** Сводка по торговым точкам. */
    suspend fun byPlace(filter: SalesFilter): List<SalesUnit> = link.rows("$SALES/by-retail-place" + filter.query())

    /** Доставка документов за срок. */
    suspend fun delivery(filter: SalesFilter): SalesDelivery = link.get("$SALES/documents" + filter.query())

    /**
     * Все разрезы срока разом: шесть запросов параллельно, первый отказ
     * отменяет остальные. Частичного ответа нет — числа за один срок, а
     * таблица за другой хуже честной неудачи.
     */
    suspend fun sales(filter: SalesFilter): SalesFigures = coroutineScope {
        val summary = async { summary(filter) }
        val days = async { byDay(filter) }
        val hours = async { byHour(filter) }
        val registers = async { byRegister(filter) }
        val places = async { byPlace(filter) }
        val delivery = async { delivery(filter) }
        SalesFigures(summary.await(), days.await(), hours.await(), registers.await(), places.await(), delivery.await())
    }

    private companion object {
        const val REGISTERS = "/api/analytics/cash-registers"
        const val SALES = "/api/analytics/sales"
    }
}

/** Отбор строкой запроса; незаданные точка и касса не пишутся. */
private fun SalesFilter.query(): String =
    query("from" to from, "to" to to, "retailPlaceId" to retailPlaceId, "cashRegisterId" to cashRegisterId)
