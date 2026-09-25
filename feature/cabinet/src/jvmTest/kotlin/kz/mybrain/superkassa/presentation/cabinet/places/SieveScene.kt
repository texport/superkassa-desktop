package kz.mybrain.superkassa.presentation.cabinet.places

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceRef
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceRow
import kz.mybrain.superkassa.presentation.cabinet.places.component.placeRows

/**
 * Хозяйство для проверок отбора и порядка колонки точек: три точки и шесть
 * касс всех смыслов учёта.
 */
internal object SieveScene {

    val allPlaces = listOf(
        // Названия и адреса нарочно вразнобой: по порядку списка кабинета
        // ни название, ни адрес, ни число касс не возрастают.
        place("p1", "Магазин на Абая", "Алматы, Абая, 10", registers = 3),
        place("p2", "Склад у вокзала", "Алматы, Жандосова, 4", registers = 1),
        place("p3", "Ларёк в парке", "Астана, Кенесары, 7", registers = 2)
    )

    val allRegisters = listOf(
        register("r1", "p1", "REGISTERED"),
        register("r2", "p1", "DRAFT"),
        register("r3", "p1", "REGISTRATION_IN_ISNA_ERROR"),
        register("r4", "p2", "DEREGISTERED"),
        register("r5", "p3", "REGISTRATION_IN_ISNA_PROCESS"),
        register("r6", "p3", "DRAFT")
    )

    fun rows(sieve: PlaceSieve, open: String? = null, locked: Set<String> = emptySet()) =
        placeRows(allPlaces, allRegisters, open, sieve, locked)

    fun names(sieve: PlaceSieve, open: String? = null, locked: Set<String> = emptySet()) =
        rows(sieve, open, locked).filterIsInstance<PlaceRow.Point>().map { it.place.name }

    fun kkms(sieve: PlaceSieve, open: String?, locked: Set<String> = emptySet()) =
        rows(sieve, open, locked).filterIsInstance<PlaceRow.Register>().map { it.register.id }

    fun place(id: String, name: String, address: String, registers: Long) = RetailPlace(
        id = id,
        name = name,
        address = address,
        addressKz = address,
        cashRegisterCount = registers
    )

    fun register(id: String, place: String, status: String) = CabinetRegister(
        id = id,
        kkmId = id.hashCode(),
        internalName = "Касса $id",
        status = status,
        registrationNumber = "%012d".format(id.drop(1).toLong()),
        retailPlace = RetailPlaceRef(place)
    )
}
