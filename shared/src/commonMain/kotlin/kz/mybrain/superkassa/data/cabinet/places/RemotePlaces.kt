package kz.mybrain.superkassa.data.cabinet.places

import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.data.cabinet.toCabinet
import kz.mybrain.superkassa.domain.cabinet.model.ChangeAddressResult
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceCreate
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces
import kz.mybrain.superkassa.integrations.bfdcabinet.places.PlacesApi
import kz.mybrain.superkassa.integrations.bfdcabinet.places.RetailPlaceAddress as BfdAddress
import kz.mybrain.superkassa.integrations.bfdcabinet.places.RetailPlaceCreate as BfdCreate

/** Торговые точки — модулем кабинета от имени вошедшего. */
internal class RemotePlaces(private val places: PlacesApi) : CabinetPlaces {
    override suspend fun all(onPart: (List<RetailPlace>, Long) -> Unit): List<RetailPlace> =
        cabinetCall { places.all { part, total -> onPart(part.map { it.place() }, total) } }.map { it.place() }

    override suspend fun add(place: RetailPlaceCreate): RetailPlace {
        val create = BfdCreate(place.name, place.addressRef, place.latitude.toCabinet(), place.longitude.toCabinet())
        return cabinetCall { places.add(create) }.place()
    }

    override suspend fun rename(id: String, name: String): RetailPlace = cabinetCall { places.rename(id, name) }.place()

    override suspend fun move(id: String, address: RetailPlaceAddress): ChangeAddressResult {
        val moved = BfdAddress(address.addressRef, address.latitude.toCabinet(), address.longitude.toCabinet())
        return cabinetCall { places.move(id, moved) }.result()
    }

    override suspend fun remove(id: String) = cabinetCall { places.remove(id) }
}
