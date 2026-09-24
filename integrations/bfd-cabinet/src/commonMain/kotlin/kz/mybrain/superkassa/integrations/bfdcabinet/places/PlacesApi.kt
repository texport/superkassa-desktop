package kz.mybrain.superkassa.integrations.bfdcabinet.places

import io.ktor.http.HttpMethod
import kz.mybrain.superkassa.integrations.bfdcabinet.CABINET_PAGE_SIZE
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetPage
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.allPages
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.query

/**
 * Торговые точки компании.
 *
 * Адрес точки берётся из адресного регистра кабинета:
 * произвольный адрес кабинет не примет.
 */
class PlacesApi internal constructor(private val link: CabinetLink) {

    /** Одна страница точек. */
    suspend fun page(page: Int = 0): CabinetPage<RetailPlace> =
        link.get(BASE + query("page" to page, "size" to CABINET_PAGE_SIZE))

    /**
     * Первая страница точек, найденных кабинетом по названию или адресу.
     *
     * Для выбора точки в форме: кабинет ищет сам, и приложению не нужно
     * читать ради одной точки все сорок страниц сети.
     *
     * @param text что набрано; пусто — первые точки компании.
     */
    suspend fun search(text: String): CabinetPage<RetailPlace> =
        link.get(BASE + query("query" to text.trim(), "page" to 0, "size" to CABINET_PAGE_SIZE))

    /**
     * Все точки компании, страница за страницей.
     *
     * @param onPart прочитанное на сейчас и сколько точек всего: у сети их
     *   тысячи, и первые можно показать, не дожидаясь последних.
     */
    suspend fun all(onPart: (List<RetailPlace>, Long) -> Unit = { _, _ -> }): List<RetailPlace> =
        allPages({ page(it) }, onPart)

    /** Заводит точку. */
    suspend fun add(place: RetailPlaceCreate): RetailPlace = link.send(HttpMethod.Post, BASE, place)

    /** Переименовывает точку. */
    suspend fun rename(id: String, name: String): RetailPlace =
        link.send(HttpMethod.Patch, "$BASE/${id.inPath()}", RetailPlaceRename(name))

    /** Меняет адрес точки — или объясняет, почему нельзя. */
    suspend fun move(id: String, address: RetailPlaceAddress): ChangeAddressResult =
        link.send(HttpMethod.Put, "$BASE/${id.inPath()}/address", address)

    /** Удаляет точку. */
    suspend fun remove(id: String) = link.done(HttpMethod.Delete, "$BASE/${id.inPath()}")

    private companion object {
        const val BASE = "/api/retail-places"
    }
}
