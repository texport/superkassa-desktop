package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Торговые точки компании и справочники, из которых они собираются.
 *
 * Адрес точки берётся из регистра, модель кассы — из справочника ИСНА:
 * произвольные значения ни та ни другая служба не примут.
 */

// --- Торговые точки ---

suspend fun CabinetClient.retailPlaces(token: String, page: Int = 0): CabinetPage<RetailPlace> =
    request(HttpMethod.Get, "/api/retail-places?page=$page&size=$PAGE_SIZE", token = token)

/**
 * Все торговые точки компании.
 *
 * По списку идёт поиск в колонке и выбор точки в заявлении, поэтому он
 * читается целиком: у сети их тысячи, а страница кабинета — пятьдесят.
 */
suspend fun CabinetClient.allRetailPlaces(
    token: String,
    onPart: (List<RetailPlace>, Long) -> Unit = { _, _ -> }
): List<RetailPlace> = allPages({ page -> retailPlaces(token, page) }, onPart)

suspend fun CabinetClient.addRetailPlace(token: String, place: RetailPlaceCreate): RetailPlace =
    request(HttpMethod.Post, "/api/retail-places", place, token)

suspend fun CabinetClient.renameRetailPlace(token: String, id: String, name: String): RetailPlace =
    request(HttpMethod.Patch, "/api/retail-places/$id", RetailPlaceRename(name), token)

suspend fun CabinetClient.moveRetailPlace(
    token: String,
    id: String,
    address: RetailPlaceAddress
): ChangeAddressResult = request(HttpMethod.Put, "/api/retail-places/$id/address", address, token)

suspend fun CabinetClient.removeRetailPlace(token: String, id: String) {
    call(HttpMethod.Delete, "/api/retail-places/$id", null, token)
}

internal fun String.encoded(): String =
    java.net.URLEncoder.encode(this, Charsets.UTF_8)
