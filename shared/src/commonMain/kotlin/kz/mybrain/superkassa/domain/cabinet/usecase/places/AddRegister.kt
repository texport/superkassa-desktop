package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegisterCreate
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/**
 * Заводит кассу в кабинете черновиком: на учёт её ставит заявление в ИСНА.
 * Заводской номер и своё название уходят без пробелов по краям.
 */
class AddRegister(private val registers: CabinetRegisters) {
    suspend operator fun invoke(register: RegisterCreate): CabinetRegister = registers.add(
        register.copy(
            factoryNumber = register.factoryNumber.trim(),
            internalName = register.internalName?.trim()?.takeIf { it.isNotBlank() }
        )
    )
}
