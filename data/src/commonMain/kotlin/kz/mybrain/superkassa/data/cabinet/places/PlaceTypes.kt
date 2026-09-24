package kz.mybrain.superkassa.data.cabinet.places

import kz.mybrain.superkassa.data.cabinet.toDecimal
import kz.mybrain.superkassa.domain.cabinet.model.BlockingRegister
import kz.mybrain.superkassa.domain.cabinet.model.ChangeAddressResult
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.integrations.bfdcabinet.places.BlockingRegister as BfdBlocking
import kz.mybrain.superkassa.integrations.bfdcabinet.places.ChangeAddressResult as BfdResult
import kz.mybrain.superkassa.integrations.bfdcabinet.places.RetailPlace as BfdPlace

/** Точка кабинета — точкой предметной области. */
internal fun BfdPlace.place() = RetailPlace(
    id = id,
    name = name,
    addressRef = addressRef,
    rka = rka,
    cato = cato,
    address = address,
    addressKz = addressKz,
    latitude = latitude?.toDecimal(),
    longitude = longitude?.toDecimal(),
    cashRegisterCount = cashRegisterCount
)

/** Ответ на смену адреса: сменён или какие кассы мешают. */
internal fun BfdResult.result() = ChangeAddressResult(
    retailPlaceId = retailPlaceId,
    updated = updated,
    changeMode = changeMode,
    blockingCashRegisters = blockingCashRegisters.map { it.blocking() }
)

private fun BfdBlocking.blocking() = BlockingRegister(id, internalName, registrationNumber, factoryNumber)
