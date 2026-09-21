package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Справочники кабинета: адресный регистр, модели касс и классификатор ОКЭД.
 */

// --- Справочники ---

/**
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

/**
 * Классификатор видов экономической деятельности.
 *
 * Кабинет держит его у себя: 2107 позиций НК РК 03-2019 с наименованиями
 * на русском и казахском. Отбор идёт сначала по коду, затем по наименованию,
 * подробные уровни впереди разделов — владелец ищет свой вид деятельности,
 * а не раздел, в который тот входит. Пустой запрос отдаёт начало
 * классификатора, поэтому параметр не передаётся вовсе.
 *
 * @param from смещение в выдаче: страница за страницей, а не первые
 *   пятьдесят. Всего подходящих кабинет сообщает в `total`.
 */
suspend fun CabinetClient.okedSuggestions(
    token: String,
    query: String,
    from: Int = 0
): OkedSuggestions = request(
    HttpMethod.Get,
    "/api/reference/okeds?${query.asQuery()}limit=$OKEDS&offset=$from",
    token = token
)

/** Одна позиция классификатора по коду: подтверждение выбранного кода. */
suspend fun CabinetClient.okedByCode(token: String, code: String): OkedEntry =
    request(HttpMethod.Get, "/api/reference/okeds/${code.encoded()}", token = token)

suspend fun CabinetClient.kkmModels(token: String): CabinetPage<KkmModel> =
    request(HttpMethod.Get, "/api/reference/kkm-models?page=0&size=$PAGE_SIZE", token = token)

/** Сколько подсказок просить у адресного регистра на одном шаге; предел регистра — 50. */
private const val SUGGESTIONS = 20

/**
 * Размер страницы классификатора видов деятельности.
 *
 * Классификатор — 2107 позиций, и просить их все нельзя: кабинет
 * ограничивает страницу пятьюдесятью. Прежде здесь стояло то же число,
 * что и для шагов адресного регистра, — двадцать, — и страниц не было
 * вовсе: доскроллить до своего вида не получалось ни при каком запросе.
 */
internal const val OKEDS = 50

/** Для вопроса «есть ли вложенные» хватает одной записи. */
private const val NESTED_PROBE = 1

/** Параметр поиска с амперсандом, либо ничего: пустой параметр кабинет отвергает. */
private fun String.asQuery(name: String = "query"): String =
    if (isBlank()) "" else "$name=${encoded()}&"
