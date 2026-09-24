package kz.mybrain.superkassa.domain.kassa.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.SaleSeat
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Смена и отрасль кассы для продажи.
 *
 * Смену называет касса; не ответила — берётся признак из сведений о кассе:
 * он мог устареть, но выдумывать закрытую смену хуже. Отрасль — настройка
 * рабочего места.
 */
class ReadSaleSeat(private val kassa: Kassa, private val signed: SignedKkm, private val memory: WorkplaceMemory) {

    /** @param kkm касса, как её знает вход: её признак смены — запасной ответ. */
    suspend operator fun invoke(kkm: KkmResponse): SaleSeat {
        val shift = kassa.askSeated(signed) { api, seat -> api.getLocalOpenShift(seat.kkmId, seat.pin) }
        val open = (shift as? Answer.Done)?.let { it.value != null } ?: kkm.isShiftOpen
        return SaleSeat(open, memory.domain(kkm.kkmId))
    }
}
