package kz.mybrain.superkassa.domain.setup.usecase

import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.domain.setup.port.SetupCabinet

/**
 * Касса в кабинете: встала ли она на учёт.
 *
 * По ответу мастер решает, пройден ли шаг заявления и можно ли заводить
 * кассу на рабочем месте: токен выпускается только кассе на учёте.
 */
class ReadCabinetRecord(private val cabinet: SetupCabinet) {
    suspend operator fun invoke(registerId: String): CabinetRecord = cabinet.record(registerId)
}
