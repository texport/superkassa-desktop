package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.TillReference
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Справочники кассы: виды оплаты и ставки НДС.
 *
 * Молча: не прочитанный справочник — не беда кассира, экран возьмёт свой.
 */
class ReadTillReference(private val kassa: Kassa) {
    suspend operator fun invoke(): TillReference = TillReference(
        paymentTypes = (kassa.ask { it.getPaymentTypes() } as? Answer.Done)?.value.orEmpty(),
        vatRates = (kassa.ask { it.listVatRates() } as? Answer.Done)?.value.orEmpty()
    )
}
