package kz.mybrain.superkassa.domain.cabinet.port

import kz.mybrain.superkassa.domain.cabinet.model.ChangeAddressResult
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceCreate

/**
 * Торговые точки компании.
 *
 * Список читается целиком, страница за страницей: по нему идёт поиск
 * в колонке и выбор в заявлении, и оборванный на первой странице список
 * ответил бы «ничего не нашлось» о точке, которая у владельца есть.
 */
interface CabinetPlaces {

    /** Все точки; [onPart] получает прочитанное после каждой страницы и сколько их всего. */
    suspend fun all(onPart: (List<RetailPlace>, Long) -> Unit = { _, _ -> }): List<RetailPlace>

    suspend fun add(place: RetailPlaceCreate): RetailPlace

    suspend fun rename(id: String, name: String): RetailPlace

    /** Смена адреса: кабинет отвечает проверкой, а не точкой, — см. [ChangeAddressResult]. */
    suspend fun move(id: String, address: RetailPlaceAddress): ChangeAddressResult

    suspend fun remove(id: String)
}
