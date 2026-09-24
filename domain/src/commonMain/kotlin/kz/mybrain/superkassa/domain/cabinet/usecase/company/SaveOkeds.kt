package kz.mybrain.superkassa.domain.cabinet.usecase.company

import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCompanies

/** Заменяет набор видов деятельности целиком; отдаёт сохранённый. */
class SaveOkeds(private val company: CabinetCompanies) {
    suspend operator fun invoke(okeds: List<Oked>): List<Oked> = company.saveOkeds(okeds)
}
