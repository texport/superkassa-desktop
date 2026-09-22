package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Справочники кабинета: классификатор ОКЭД и модели касс.
 *
 * Адресный регистр живёт отдельно ([CabinetAddressApi]): он опрашивается
 * шагами, а эти два — обычными страницами.
 */

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

suspend fun CabinetClient.kkmModels(token: String, page: Int = 0): CabinetPage<KkmModel> =
    request(HttpMethod.Get, "/api/reference/kkm-models?page=$page&size=$PAGE_SIZE", token = token)

/**
 * Справочник моделей целиком.
 *
 * Модель выбирается поиском по названию и коду, и искать её нужно среди
 * всех: моделей в справочнике ИСНА сотни, а страница — пятьдесят, и своей
 * модели владелец в списке не находил.
 */
suspend fun CabinetClient.allKkmModels(token: String): List<KkmModel> =
    allPages({ page -> kkmModels(token, page) })

/**
 * Размер страницы классификатора видов деятельности.
 *
 * Классификатор — 2107 позиций, и просить их все нельзя: кабинет
 * ограничивает страницу пятьюдесятью. Прежде здесь стояло то же число,
 * что и для шагов адресного регистра, — двадцать, — и страниц не было
 * вовсе: доскроллить до своего вида не получалось ни при каком запросе.
 */
internal const val OKEDS = 50

/** Параметр поиска с амперсандом, либо ничего: пустой параметр кабинет отвергает. */
internal fun String.asQuery(name: String = "query"): String =
    if (isBlank()) "" else "$name=${encoded()}&"
