package kz.mybrain.superkassa.domain.cabinet.port

import kz.mybrain.superkassa.domain.cabinet.model.CompanyProfile
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.domain.cabinet.model.OkedEntry
import kz.mybrain.superkassa.domain.cabinet.model.OkedSuggestions

/**
 * Компания владельца и классификатор видов деятельности.
 *
 * Название и БИН приходят из ЭЦП и правке не поддаются; виды деятельности
 * владелец ведёт сам, выбирая их из классификатора ОКЭД.
 */
interface CabinetCompanies {
    suspend fun company(): CompanyProfile

    /** Заменяет набор видов деятельности целиком; отдаёт сохранённый. */
    suspend fun saveOkeds(okeds: List<Oked>): List<Oked>

    /** Страница классификатора по части кода или наименования, начиная с [from]. */
    suspend fun okeds(query: String, from: Int): OkedSuggestions

    /** Вид деятельности по коду: наименование на обоих языках. */
    suspend fun oked(code: String): OkedEntry
}
