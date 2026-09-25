package kz.mybrain.superkassa.data.cabinet

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetPage
import kz.mybrain.superkassa.integrations.bfdcabinet.register.CashRegisterModel
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RetailPlaceRef
import kz.mybrain.superkassa.integrations.bfdcabinet.places.RetailPlace as BfdPlace
import kz.mybrain.superkassa.integrations.bfdcabinet.register.CabinetRegister as BfdRegister

/**
 * Точки и кассы проверки — так, как их отдаёт кабинет: страницей в JSON.
 *
 * Сцены собирают хозяйство моделями предметной области, а подставной
 * кабинет отдаёт его своим договором — тем же, что читает модуль кабинета.
 */
object CabinetPages {
    private val json = Json { explicitNulls = false }

    fun places(items: List<RetailPlace>): String = page(BfdPlace.serializer(), items.map { it.wire() })

    fun registers(items: List<CabinetRegister>): String = page(BfdRegister.serializer(), items.map { it.wire() })

    private fun <T> page(item: KSerializer<T>, items: List<T>): String = json.encodeToString(
        CabinetPage.serializer(item),
        CabinetPage(page = 0, size = items.size, totalElements = items.size.toLong(), items = items)
    )

    private fun RetailPlace.wire() = BfdPlace(
        id = id,
        name = name,
        addressRef = addressRef,
        rka = rka,
        cato = cato,
        address = address,
        addressKz = addressKz,
        latitude = latitude?.toCabinet(),
        longitude = longitude?.toCabinet(),
        cashRegisterCount = cashRegisterCount
    )

    private fun CabinetRegister.wire() = BfdRegister(
        id = id,
        kkmId = kkmId,
        internalName = internalName,
        status = status,
        registrationNumber = registrationNumber,
        factoryNumber = factoryNumber,
        manufactureYear = manufactureYear,
        modelView = model?.let { CashRegisterModel(it.modelCode, it.name) },
        retailPlaceView = retailPlace?.let { RetailPlaceRef(it.id, it.name) },
        registrationCardAvailable = registrationCardAvailable
    )
}
