package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/**
 * Карточка кассы: в ней есть то, чего нет в строке списка, — признак
 * регистрационной карты и последнее действие.
 */
class ReadRegisterCard(private val registers: CabinetRegisters) {
    suspend operator fun invoke(id: String): CabinetRegister = registers.one(id)
}
