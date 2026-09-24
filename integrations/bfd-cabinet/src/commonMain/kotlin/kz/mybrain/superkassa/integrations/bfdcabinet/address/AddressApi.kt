package kz.mybrain.superkassa.integrations.bfdcabinet.address

import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.query

/**
 * Адресный регистр КГД: откуда берётся адрес торговой точки.
 *
 * Адрес выбирается шагами — регион, населённый пункт, улица, строение;
 * свободного поиска по адресу целиком регистр не даёт. Пустой запрос
 * не передаётся вовсе: пустой параметр кабинет отвергает, а без параметра
 * отдаёт первые записи шага.
 */
class AddressApi internal constructor(private val link: CabinetLink) {

    /** Регионы. */
    suspend fun regions(query: String): List<AddressSuggestion> =
        step("regions" + query("query" to query, "limit" to SUGGESTIONS))

    /** Населённые пункты региона или пункта. */
    suspend fun localities(parentId: Long, query: String): List<AddressSuggestion> =
        step("localities" + query("parentId" to parentId, "query" to query, "limit" to SUGGESTIONS))

    /** Вложенные пункты: у городов с районами улицы лежат под районами. Для вопроса «есть ли» хватает одной. */
    suspend fun nested(localityId: Long): List<AddressSuggestion> =
        step("localities" + query("parentId" to localityId, "limit" to 1))

    /** Улицы пункта. */
    suspend fun streets(localityId: Long, query: String): List<AddressSuggestion> =
        step("streets" + query("atsId" to localityId, "query" to query, "limit" to SUGGESTIONS))

    /** Строения улицы по номеру. */
    suspend fun buildings(streetId: Long, number: String): List<AddressSuggestion> =
        step("buildings" + query("geonimId" to streetId, "number" to number, "limit" to SUGGESTIONS))

    /** Готовый адрес по коду РКА выбранного строения. */
    suspend fun resolve(rka: String): RegisterAddress = link.get<ResolvedAddress>("$BASE/${rka.inPath()}").address()

    private suspend fun step(tail: String): List<AddressSuggestion> = link.get<AddressSuggestions>("$BASE/$tail").items

    private companion object {
        const val BASE = "/api/reference/addresses"

        /** Подсказок на одном шаге; предел регистра — 50. */
        const val SUGGESTIONS = 20
    }
}
