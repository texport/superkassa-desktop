package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kz.mybrain.superkassa.domain.cabinet.model.TokenIssued
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/** Выпускает новый технический токен кассы: прежний БФД отзывает. */
class IssueToken(private val registers: CabinetRegisters) {
    suspend operator fun invoke(id: String): TokenIssued = registers.issueToken(id)
}
