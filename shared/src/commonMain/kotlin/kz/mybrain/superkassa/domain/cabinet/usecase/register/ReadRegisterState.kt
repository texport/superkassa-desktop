package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/** Состояние кассы по учёту кабинета и по снимку БФД. */
class ReadRegisterState(private val registers: CabinetRegisters) {
    suspend operator fun invoke(id: String): RegisterState = registers.state(id)
}
