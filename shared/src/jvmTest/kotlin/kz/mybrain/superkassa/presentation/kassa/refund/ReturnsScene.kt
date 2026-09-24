package kz.mybrain.superkassa.presentation.kassa.refund

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.CounterSnapshotResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.DocumentDetailsResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScene

/**
 * Касса для проверок возврата: день чеков, строки основания, ящик.
 *
 * Модель возврата работает на ней так же, как на настоящей кассе:
 * читает день, по выбранному чеку — его строки, и остаток ящика.
 * Выбор чека в снимках делается нажатием, как у кассира.
 */
object ReturnsScene {

    fun model(
        documents: List<FiscalDocumentResponse>,
        items: List<ReceiptItemView> = emptyList(),
        drawer: Long = 125_000,
        payments: List<PaymentTypeResponse> = emptyList()
    ): ReturnsViewModel {
        val core = core(documents, items, drawer, payments)
        return returnsModel(CoreScene.services(core, SaleScene.signedIn()), KassaPorts(FixedDeliverySetup()))
    }

    fun core(
        documents: List<FiscalDocumentResponse>,
        items: List<ReceiptItemView> = emptyList(),
        drawer: Long = 125_000,
        payments: List<PaymentTypeResponse> = emptyList(),
        shift: ShiftResponse? = CoreScene.openShift()
    ): FakeCore = SaleScene.core(shift).apply {
        on("getPaymentTypes") { payments }
        on(
            "listCounters"
        ) { listOf(CounterSnapshotResponse(scope = "GLOBAL", key = "cash.sum", value = drawer, updatedAt = 0)) }
        on("listFiscalDocumentsByPeriod") { args -> if (args[4] == 0) documents else emptyList() }
        on("getDocumentDetails") { args ->
            DocumentDetailsResponse(documents.first { it.id == args[1] }, items, operatorName = "Айгүл Сәрсенова")
        }
    }

    /** Строка чека-основания: цена и сумма в тенге, количество в тысячных. */
    fun item(name: String, price: String, thousandths: Long, sum: String, unit: String = "796") = ReceiptItemView(
        name = name,
        price = Decimal.parse(price),
        quantityThousandths = thousandths,
        sum = Decimal.parse(sum),
        vatGroup = "VAT_16",
        measureUnitCode = unit
    )

    /** Вид оплаты кассы; непринимаемый гаснет в списке. */
    fun payment(code: String, supported: Boolean = true) =
        PaymentTypeResponse(code, TrilingualMessageResponse(code, code, code), supported)

    /** Чек продажи с номером на бумаге и фискальным признаком. */
    fun sale(no: Long, tiyn: Long): FiscalDocumentResponse =
        CoreScene.document("sale-$no", amount = tiyn, status = "SENT").copy(
            docNo = no,
            printedDocumentNumber = no,
            fiscalSign = "38%06d".format(no),
            createdAt = System.currentTimeMillis() - no * 900_000
        )
}
