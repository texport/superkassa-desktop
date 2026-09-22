package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Адресный регистр: откуда берётся адрес торговой точки.
 *
 * Отдельно от прочих справочников кабинета ([CabinetReferenceApi]):
 * адрес выбирается шагами и своими ручками, а классификатор видов
 * деятельности и модели касс — обычными страницами.
 *
 * Адрес выбирается по шагам: регион → населённый пункт → улица → строение.
 * Свободного поиска по адресу целиком адресный регистр не даёт, а без
 * пустого параметра `query` кабинет отвечает ошибкой, а без параметра
 * отдаёт первые записи шага — поэтому пустой запрос не передаётся вовсе.
 */

suspend fun CabinetClient.addressRegions(token: String, query: String): AddressSuggestions =
    request(HttpMethod.Get, "/api/reference/addresses/regions?${query.asQuery()}limit=$SUGGESTIONS", token = token)

suspend fun CabinetClient.addressLocalities(token: String, regionId: Long, query: String): AddressSuggestions =
    request(
        HttpMethod.Get,
        "/api/reference/addresses/localities?parentId=$regionId&${query.asQuery()}limit=$SUGGESTIONS",
        token = token
    )

/** Есть ли у пункта вложенные пункты: у городов с районами улицы лежат под районами. */
suspend fun CabinetClient.addressNestedLocalities(token: String, localityId: Long): AddressSuggestions =
    request(HttpMethod.Get, "/api/reference/addresses/localities?parentId=$localityId&limit=$NESTED_PROBE", token = token)

suspend fun CabinetClient.addressStreets(token: String, localityId: Long, query: String): AddressSuggestions =
    request(
        HttpMethod.Get,
        "/api/reference/addresses/streets?atsId=$localityId&${query.asQuery()}limit=$SUGGESTIONS",
        token = token
    )

suspend fun CabinetClient.addressBuildings(token: String, streetId: Long, number: String): AddressSuggestions =
    request(
        HttpMethod.Get,
        "/api/reference/addresses/buildings?geonimId=$streetId&${number.asQuery("number")}limit=$SUGGESTIONS",
        token = token
    )

/** Готовый адрес по коду РКА выбранного строения: то, что уходит в точку как `addressRef`. */
suspend fun CabinetClient.resolveAddress(token: String, rka: String): RegisterAddress =
    request<ResolvedAddress>(HttpMethod.Get, "/api/reference/addresses/${rka.encoded()}", token = token).toRegisterAddress()

/** Сколько подсказок просить у адресного регистра на одном шаге; предел регистра — 50. */
private const val SUGGESTIONS = 20

/** Для вопроса «есть ли вложенные» хватает одной записи. */
private const val NESTED_PROBE = 1
