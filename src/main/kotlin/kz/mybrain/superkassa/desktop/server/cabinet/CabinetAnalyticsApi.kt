package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Аналитика по кассам компании: карта и адреса обмена.
 *
 * Ручки кабинета, а не свои расчёты: кабинет знает и учётные сведения
 * касс, и то, что прислали сами машины. Приложение добавляет к этому
 * одно — координаты по адресу торговой точки, которых у кабинета нет.
 */

/** Кассы на карте: положение берётся из выбранного источника. */
suspend fun CabinetClient.cashRegisterMap(
    token: String,
    source: PositionSource,
    retailPlaceId: String? = null,
    status: String? = null
): KkmMapView = request(HttpMethod.Get, "$ANALYTICS/map${mapQuery(source, retailPlaceId, status)}", token = token)

/** Адреса, с которых выходили на связь все кассы компании. */
suspend fun CabinetClient.exchangeAddresses(token: String): ExchangeAddresses =
    request(HttpMethod.Get, "$ANALYTICS/addresses", token = token)

/** То же по одной кассе: у кассы без обменов ответ пустой, а не отказ. */
suspend fun CabinetClient.exchangeAddresses(token: String, registerId: String): ExchangeAddresses =
    request(HttpMethod.Get, "$ANALYTICS/${registerId.encoded()}/addresses", token = token)

/** Отбор карты строкой запроса: незаданное не пишется вовсе. */
internal fun mapQuery(source: PositionSource, retailPlaceId: String?, status: String?): String {
    val parts = listOfNotNull(
        "positionSource=${source.code}",
        retailPlaceId?.takeIf { it.isNotBlank() }?.let { "retailPlaceId=${it.encoded()}" },
        status?.takeIf { it.isNotBlank() }?.let { "status=${it.encoded()}" }
    )
    return "?" + parts.joinToString("&")
}

/** Основание всех ручек аналитики. */
private const val ANALYTICS = "/api/analytics/cash-registers"
