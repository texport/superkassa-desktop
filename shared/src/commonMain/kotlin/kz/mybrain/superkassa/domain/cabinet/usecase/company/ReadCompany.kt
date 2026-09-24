package kz.mybrain.superkassa.domain.cabinet.usecase.company

import kz.mybrain.superkassa.domain.cabinet.model.CompanyProfile
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCompanies

/** Карточка компании владельца с её видами деятельности. */
class ReadCompany(private val company: CabinetCompanies) {
    suspend operator fun invoke(): CompanyProfile = company.company()
}
