package kz.mybrain.superkassa.data.cabinet.address

import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAddresses
import kz.mybrain.superkassa.integrations.bfdcabinet.address.AddressApi
import kz.mybrain.superkassa.integrations.bfdcabinet.address.AddressSuggestion as BfdSuggestion

/** Адресный регистр КГД — модулем кабинета от имени вошедшего. */
internal class RemoteAddresses(private val addresses: AddressApi) : CabinetAddresses {
    override suspend fun regions(query: String): List<AddressSuggestion> =
        cabinetCall { addresses.regions(query) }.map { it.suggestion() }

    override suspend fun localities(parentId: Long, query: String): List<AddressSuggestion> =
        cabinetCall { addresses.localities(parentId, query) }.map { it.suggestion() }

    override suspend fun nested(localityId: Long): List<AddressSuggestion> =
        cabinetCall { addresses.nested(localityId) }.map { it.suggestion() }

    override suspend fun streets(localityId: Long, query: String): List<AddressSuggestion> =
        cabinetCall { addresses.streets(localityId, query) }.map { it.suggestion() }

    override suspend fun buildings(streetId: Long, number: String): List<AddressSuggestion> =
        cabinetCall { addresses.buildings(streetId, number) }.map { it.suggestion() }

    override suspend fun resolve(rka: String): RegisterAddress = cabinetCall { addresses.resolve(rka) }
        .let { RegisterAddress(it.addressRef, it.address, it.addressKz, it.rka, it.cato) }
}

private fun BfdSuggestion.suggestion() = AddressSuggestion(id, name, rka, level)
