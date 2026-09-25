package kz.mybrain.superkassa.domain.cabinet

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceRef

/**
 * Составы раздела торговых точек для снимков.
 *
 * Точка бывает без адреса, без координат и с десятком касс под собой,
 * и все три состояния нужны сразу нескольким снимкам: собранные в каждом
 * заново, они разошлись бы мелочами.
 */
object PlaceLook {

    /** Точка с адресом и координатами — обычная, какой её заводят. */
    fun place(
        at: Int,
        address: String? = "г. Алматы, пр. Абая, $at",
        point: Boolean = true,
        registers: Long = 2
    ) = RetailPlace(
        id = "p$at",
        name = "Магазин на Абая $at",
        addressRef = address?.let { "RKA-$at" },
        rka = address?.let { "%010d".format(at.toLong()) },
        cato = "751310000",
        address = address,
        addressKz = address?.let { "Алматы қ., Абай даң., $at" },
        latitude = if (point) Decimal.parse("43.238949") else null,
        longitude = if (point) Decimal.parse("76.889709") else null,
        cashRegisterCount = registers
    )

    /** Касса под точкой: на учёте или снятая с него. */
    fun register(at: Int, place: String, onRecord: Boolean = true) = CabinetRegister(
        id = "r$at",
        kkmId = 2000300 + at,
        internalName = "Касса $at",
        status = if (onRecord) "REGISTERED" else "DEREGISTERED",
        registrationNumber = "%012d".format(4500000L + at),
        factoryNumber = "SK-$at",
        retailPlace = RetailPlaceRef(place)
    )
}
