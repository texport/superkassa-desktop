package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.shift.ReportResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Снимает X-отчёт открытой смены.
 *
 * Отдаёт ответ кассы целиком: состояние доставки в БФД, а при отказе —
 * причину словами БФД и её код. Прежде отдавалось одно состояние,
 * и кассир видел «отклонён» без причины.
 */
class TakeXReport(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(): Answer<ReportResponse> =
        kassa.askSeated(signed) { api, seat -> api.createReport(seat.kkmId, seat.pin) }
}
