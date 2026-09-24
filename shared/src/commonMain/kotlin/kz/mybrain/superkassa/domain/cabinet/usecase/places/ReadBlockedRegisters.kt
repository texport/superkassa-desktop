package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/** Какие кассы кабинет считает заблокированными: сводкой на всю компанию. */
class ReadBlockedRegisters(private val registers: CabinetRegisters) {
    suspend operator fun invoke(): Set<String> = registers.blocked()
}
