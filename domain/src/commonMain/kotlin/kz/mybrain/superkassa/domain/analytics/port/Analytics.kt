package kz.mybrain.superkassa.domain.analytics.port

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddresses
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesFilter
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary

/**
 * Аналитика кабинета по кассам компании: где стоят, откуда выходят
 * на связь и чем торгуют.
 *
 * Считает кабинет: и учётные сведения касс, и присланное самими машинами
 * лежат у него. Приложение спрашивает и показывает.
 *
 * Каждое обращение идёт от имени владельца, вошедшего в кабинет; кто вошёл,
 * знает реализация — сценарии аналитики доступа не видят.
 *
 * Ответ — [AnalyticsAnswer]: помеха аналитики говорится на самом экране,
 * а не всплывающей строкой, и путать её виды нельзя.
 */
interface Analytics {

    /** Кассы компании с положением из выбранного источника. */
    suspend fun kkms(source: PositionSource): AnalyticsAnswer<KkmMapView>

    /** Адреса, с которых выходили на связь все кассы компании. */
    suspend fun exchange(): AnalyticsAnswer<ExchangeAddresses>

    /**
     * Торговая сводка срока: все её разрезы разом.
     *
     * Частичного ответа не бывает: плитки за одну неделю, а таблица под
     * ними за другую — хуже честной помехи на весь экран.
     */
    suspend fun sales(filter: SalesFilter): AnalyticsAnswer<SalesFigures>

    /** Одни главные числа срока — для сравнения с прошлым сроком. */
    suspend fun summary(filter: SalesFilter): AnalyticsAnswer<SalesSummary>

    /** Торговые точки компании с адресами: по ним выручка складывается в регионы. */
    suspend fun places(): AnalyticsAnswer<List<PlaceAddress>>
}
