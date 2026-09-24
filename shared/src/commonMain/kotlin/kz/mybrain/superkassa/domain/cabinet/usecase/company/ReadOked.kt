package kz.mybrain.superkassa.domain.cabinet.usecase.company

import kz.mybrain.superkassa.domain.cabinet.model.OkedEntry
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCompanies

/** Вид деятельности по коду: наименование на обоих языках. */
class ReadOked(private val company: CabinetCompanies) {
    suspend operator fun invoke(code: String): OkedEntry = company.oked(code)
}
