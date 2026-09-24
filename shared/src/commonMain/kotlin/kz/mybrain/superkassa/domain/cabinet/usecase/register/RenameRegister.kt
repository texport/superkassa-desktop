package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegisterName
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/**
 * Своё название кассы у владельца: заметка для него, в ОФД не уходит.
 *
 * @return касса с новым названием; `null` — название не годится (пустое
 *   или длиннее предела), и к кабинету с ним не ходили: он его не примет.
 */
class RenameRegister(private val registers: CabinetRegisters) {
    suspend operator fun invoke(id: String, name: String): CabinetRegister? =
        RegisterName.of(name)?.let { registers.rename(id, it) }
}
