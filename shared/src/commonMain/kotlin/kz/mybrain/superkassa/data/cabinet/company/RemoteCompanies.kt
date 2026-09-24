package kz.mybrain.superkassa.data.cabinet.company

import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.domain.cabinet.model.CompanyProfile
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.domain.cabinet.model.OkedEntry
import kz.mybrain.superkassa.domain.cabinet.model.OkedSuggestions
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCompanies
import kz.mybrain.superkassa.integrations.bfdcabinet.company.CompanyApi
import kz.mybrain.superkassa.integrations.bfdcabinet.company.CompanyProfile as BfdCompany
import kz.mybrain.superkassa.integrations.bfdcabinet.company.Oked as BfdOked
import kz.mybrain.superkassa.integrations.bfdcabinet.company.OkedEntry as BfdOkedEntry

/** Компания и классификатор видов деятельности — модулем кабинета. */
internal class RemoteCompanies(private val company: CompanyApi) : CabinetCompanies {
    override suspend fun company(): CompanyProfile = cabinetCall { company.company() }.profile()

    override suspend fun saveOkeds(okeds: List<Oked>): List<Oked> =
        cabinetCall { company.saveOkeds(okeds.map { BfdOked(it.code, it.name, it.primary) }) }.map { it.oked() }

    override suspend fun okeds(query: String, from: Int): OkedSuggestions {
        val found = cabinetCall { company.okeds(query, from) }
        return OkedSuggestions(found.items.map { it.entry() }, found.total)
    }

    override suspend fun oked(code: String): OkedEntry = cabinetCall { company.oked(code) }.entry()
}

private fun BfdCompany.profile() = CompanyProfile(id, bin, name, okeds.map { it.oked() })

private fun BfdOked.oked() = Oked(code, name, primary)

private fun BfdOkedEntry.entry() = OkedEntry(code, name, nameKz, level)
