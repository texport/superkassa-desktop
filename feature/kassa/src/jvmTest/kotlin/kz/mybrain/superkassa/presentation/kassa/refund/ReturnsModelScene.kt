package kz.mybrain.superkassa.presentation.kassa.refund

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.ReturnsScene
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.kassa.services

/** Модель возврата над кассой проверки: чеки-основания, их строки, наличные в ящике и виды оплаты. */
internal fun ReturnsScene.model(
    documents: List<FiscalDocumentResponse>,
    items: List<ReceiptItemView> = emptyList(),
    drawer: Long = 125_000,
    payments: List<PaymentTypeResponse> = emptyList()
): ReturnsViewModel {
    val core = core(documents, items, drawer, payments)
    return returnsModel(CoreScene.services(core, SaleScene.signedIn()), KassaPorts(FixedDeliverySetup()))
}
