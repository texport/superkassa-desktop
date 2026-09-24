package kz.mybrain.superkassa.domain.cabinet.usecase.applications

import io.github.texport.superkassa.core.presentation.api.model.shift.ReportResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Закрывает смену кассы этой машины перед снятием с учёта: кабинет не
 * снимает кассу с открытой сменой. Это фискальная операция кассы, и пин
 * к ней набирает владелец.
 */
class CloseShiftHere(private val kassa: Kassa) {
    suspend operator fun invoke(kkmId: String, pin: String): Answer<ReportResponse> =
        kassa.ask { it.closeShift(kkmId, pin) }
}
