package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/** Удаляет кассу-черновик: поставленную удаляют заявлением о снятии с учёта. */
class RemoveRegister(private val registers: CabinetRegisters) {
    suspend operator fun invoke(id: String) = registers.remove(id)
}
