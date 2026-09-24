package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/**
 * Все кассы компании, страница за страницей.
 *
 * @param onPart прочитанное после каждой страницы и сколько касс всего.
 */
class ReadRegisters(private val registers: CabinetRegisters) {
    suspend operator fun invoke(onPart: (List<CabinetRegister>, Long) -> Unit): List<CabinetRegister> =
        registers.all(onPart)
}
