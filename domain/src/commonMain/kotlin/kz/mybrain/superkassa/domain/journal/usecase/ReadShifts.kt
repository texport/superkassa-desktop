package kz.mybrain.superkassa.domain.journal.usecase

import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.journal.model.DocumentPages
import kz.mybrain.superkassa.domain.journal.model.Paged
import kz.mybrain.superkassa.domain.journal.model.withPage
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Следующая страница прошлых смен кассы, за которой работают.
 *
 * Смена, открытая между двумя обращениями, сдвигает счёт страниц,
 * и повтор уже показанной отбрасывается.
 */
class ReadShifts(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return прочитанное с новой страницей. */
    suspend operator fun invoke(shown: List<ShiftResponse>): Answer<Paged<ShiftResponse>> {
        val size = DocumentPages.SHIFTS
        return kassa.askSeated(signed) { api, seat -> api.listShifts(seat.kkmId, size, shown.size, seat.pin) }
            .map { shown.withPage(it, size) { shift -> shift.id } }
    }
}
