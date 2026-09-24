package kz.mybrain.superkassa.domain.cabinet.usecase.company

import kz.mybrain.superkassa.domain.cabinet.model.OkedSuggestions
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCompanies

/**
 * Страница классификатора ОКЭД по части кода или наименования.
 * Однобуквенный запрос кабинет отвергает: ищут с пустой строки или от двух знаков.
 */
class SearchOkeds(private val company: CabinetCompanies) {
    suspend operator fun invoke(query: String, from: Int): OkedSuggestions = company.okeds(query.trim(), from)
}
