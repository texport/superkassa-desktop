package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters

/** Справочник моделей касс целиком. */
class ReadKkmModels(private val registers: CabinetRegisters) {
    suspend operator fun invoke(): List<KkmModel> = registers.models()
}
