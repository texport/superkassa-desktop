package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegisterEdit
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/** Заводской номер кассы: правится, пока касса не поставлена на учёт. */
class RestampRegister(private val registers: CabinetRegisters) {
    suspend operator fun invoke(id: String, factoryNumber: String): CabinetRegister =
        registers.edit(id, RegisterEdit(factoryNumber = factoryNumber.trim()))
}
