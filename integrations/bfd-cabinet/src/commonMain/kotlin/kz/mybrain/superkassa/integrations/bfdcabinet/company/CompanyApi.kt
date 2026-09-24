package kz.mybrain.superkassa.integrations.bfdcabinet.company

import io.ktor.http.HttpMethod
import kz.mybrain.superkassa.integrations.bfdcabinet.CABINET_PAGE_SIZE
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.query

/** Карточка компании, её виды деятельности и классификатор ОКЭД. */
class CompanyApi internal constructor(private val link: CabinetLink) {

    /** Карточка компании вошедшего. */
    suspend fun company(): CompanyProfile = link.get("/api/company")

    /**
     * Заменяет виды деятельности компании целиком.
     *
     * @return виды деятельности, как их сохранил кабинет.
     */
    suspend fun saveOkeds(okeds: List<Oked>): List<Oked> =
        link.send<OkedsRequest, OkedsView>(HttpMethod.Put, "/api/company/okeds", OkedsRequest(okeds)).okeds

    /**
     * Страница классификатора ОКЭД.
     *
     * Отбор идёт по коду и наименованию, подробные уровни впереди разделов.
     * Пустой запрос отдаёт начало классификатора и не передаётся вовсе:
     * пустой параметр кабинет отвергает.
     *
     * @param from смещение в выдаче; всего подходящих — в [OkedSuggestions.total].
     */
    suspend fun okeds(query: String, from: Int = 0): OkedSuggestions =
        link.get("/api/reference/okeds" + query("query" to query, "limit" to CABINET_PAGE_SIZE, "offset" to from))

    /** Позиция классификатора по коду: подтверждение выбранного кода. */
    suspend fun oked(code: String): OkedEntry = link.get("/api/reference/okeds/${code.inPath()}")
}
