package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.Fiscal
import kz.mybrain.superkassa.domain.kassa.model.fiscal
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundPlan
import kz.mybrain.superkassa.domain.kassa.model.refund.command
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.signin.usecase.RefreshKkm

/**
 * Пробивает чек возврата по чеку-основанию.
 *
 * Ключ — попытки возврата: повтор того же возврата после неизвестного
 * исхода идёт с ним же, и касса второго чека не пробивает. Основание без
 * номера или суммы до кассы не доходит: описать его в чеке нечем.
 */
class IssueRefund(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(plan: RefundPlan, key: String): Answer<Fiscal> {
        if (!plan.complete) return Answer.Failed(INCOMPLETE_BASIS)
        val answer = kassa.askSeated(signed) { api, seat ->
            api.createReceipt(checkNotNull(plan.command(seat.kkmId, seat.pin, key)))
        }.map { it.fiscal() }
        RefreshKkm(kassa, signed)()
        return answer
    }

    private companion object {
        /** Имя сбоя для журнала, не для кассира. */
        const val INCOMPLETE_BASIS = "IncompleteBasis"
    }
}
