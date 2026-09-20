package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Ручки торговой сводки кабинета.
 *
 * Продолжение [cashRegisterMap] и [exchangeAddresses]: там аналитика
 * отвечает на вопрос «где стоят кассы», здесь — «чем торговали».
 * Считает кабинет: чеки лежат у него.
 *
 * Все шесть ручек берут один и тот же отбор [SalesFilter] — срок,
 * и при желании точку или кассу. Разные отборы у соседних чисел
 * на одном экране означали бы, что плитки и таблицы под ними говорят
 * о разном сроке.
 */

/** Главные числа срока: чеки, выручка, возвраты, налог, виды расчётов. */
suspend fun CabinetClient.salesSummary(token: String, filter: SalesFilter): SalesSummary =
    request(HttpMethod.Get, "$SALES/summary${filter.query()}", token = token)

/** Выручка по суткам срока. */
suspend fun CabinetClient.salesByDay(token: String, filter: SalesFilter): List<SalesDay> =
    rows(token, "$SALES/by-day${filter.query()}")

/** Нагрузка по часам суток, сложенная по всему сроку. */
suspend fun CabinetClient.salesByHour(token: String, filter: SalesFilter): List<SalesHour> =
    rows(token, "$SALES/by-hour${filter.query()}")

/** Сводка по кассам компании. */
suspend fun CabinetClient.salesByCashRegister(token: String, filter: SalesFilter): List<SalesUnit> =
    rows(token, "$SALES/by-cash-register${filter.query()}")

/** Сводка по торговым точкам компании. */
suspend fun CabinetClient.salesByRetailPlace(token: String, filter: SalesFilter): List<SalesUnit> =
    rows(token, "$SALES/by-retail-place${filter.query()}")

/** Состояние доставки документов за срок. */
suspend fun CabinetClient.salesDelivery(token: String, filter: SalesFilter): SalesDelivery =
    request(HttpMethod.Get, "$SALES/documents${filter.query()}", token = token)

/** Основание ручек торговой сводки. */
private const val SALES = "/api/analytics/sales"
