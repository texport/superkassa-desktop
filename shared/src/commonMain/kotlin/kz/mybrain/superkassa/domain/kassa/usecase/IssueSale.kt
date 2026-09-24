package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.Fiscal
import kz.mybrain.superkassa.domain.kassa.model.fiscal
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleReceipt
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.signin.usecase.RefreshKkm

/**
 * Пробивает чек продажи или покупки, набранный кассиром.
 *
 * Ключ повтора — в самом чеке: повтор после неизвестного исхода с тем же
 * ключом касса отвечает прежним документом и второго не пробивает. После
 * чека касса перечитывается при любом исходе: смену и остаток видят все
 * разделы. Чек, отвергнутый БФД, приходит с кодом отказа.
 */
class IssueSale(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(receipt: SaleReceipt): Answer<Fiscal> {
        val answer = kassa.askSeated(signed) { api, seat -> api.createReceipt(receipt.command(seat.kkmId, seat.pin)) }
            .map { it.fiscal() }
        RefreshKkm(kassa, signed)()
        return answer
    }
}
