package kz.mybrain.superkassa.domain.kassa.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationRequest
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.Fiscal
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.cash.CashAttempt
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.domain.kassa.model.fiscal
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.signin.usecase.RefreshKkm

/**
 * Вносит наличные в ящик или изымает их.
 *
 * Ключ повтора — у попытки: тот же ключ касса отвечает прежней операцией
 * и второй раз деньги не проводит. После операции касса перечитывается
 * при любом исходе: остаток и смену видят шапка окна и все разделы.
 */
class MoveCash(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(attempt: CashAttempt): Answer<Fiscal> {
        val request = CashOperationRequest(Tenge.decimal(attempt.amount), attempt.key)
        val answer = kassa.askSeated(signed) { api, seat ->
            when (attempt.move) {
                CashMove.Deposit -> api.cashIn(seat.kkmId, seat.pin, request)
                CashMove.Withdraw -> api.cashOut(seat.kkmId, seat.pin, request)
            }
        }.map { it.fiscal() }
        RefreshKkm(kassa, signed)()
        return answer
    }
}
