package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureItemResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureLookupResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore

/**
 * Касса для проверок продажи, возврата и денег: смена, справочники, чеки.
 *
 * Справочники касса «не знает» — пустые, и экран берёт свои запасные;
 * смена открыта, если не сказано иное. Чеки касса принимает и помнит
 * по ключу повтора так же, как настоящая: второй чек с тем же ключом —
 * тот же документ.
 */
object SaleScene {

    fun core(shift: ShiftResponse? = CoreScene.openShift(), kkm: KkmResponse = CoreScene.kkm()) = FakeCore().apply {
        on("getPaymentTypes") { emptyList<Any>() }
        on("listVatRates") { emptyList<Any>() }
        on("getDocumentTypes") { emptyList<Any>() }
        on("getLocalOpenShift") { shift }
        on("getKkm") { kkm }
    }

    /** Кассир вошёл в кассу [kkm]. */
    fun signedIn(kkm: KkmResponse = CoreScene.kkm(), admin: Boolean = true): SignIn =
        SignIn().apply { enter(kkm, CoreScene.cashier(admin), CoreScene.PIN) }

    /**
     * Касса принимает чеки и помнит их по ключу повтора.
     *
     * @return ключи, с которыми касса звали, по порядку, и документы по ключу.
     */
    fun FakeCore.receipts(delivery: DeliveryStatus = DeliveryStatus.ONLINE_OK): Receipts {
        val receipts = Receipts()
        on("createReceipt") { args ->
            val command = args.single() as CreateReceiptCommand
            receipts.commands += command
            receipts.failing.removeFirstOrNull()?.let { throw it }
            val id = receipts.documents.getOrPut(command.idempotencyKey) { "doc-${receipts.documents.size + 1}" }
            ReceiptResponse(documentId = id, deliveryStatus = delivery)
        }
        return receipts
    }

    /** Что касса получила и что оформила. */
    class Receipts {
        val commands = mutableListOf<CreateReceiptCommand>()
        val documents = linkedMapOf<String, String>()

        /** Сбои, которыми касса ответит на ближайшие чеки, — по одному на чек. */
        val failing = ArrayDeque<Throwable>()
    }

    /** Справочник нашёл товар; цена `0` — каталог цены не несёт. */
    fun found(price: String = "450.00") = NomenclatureLookupResponse(
        found = true,
        item = NomenclatureItemResponse(
            id = 1,
            barcode = BARCODE,
            name = "Вода питьевая 0,5 л",
            nameKk = "Ауыз су 0,5 л",
            ntin = "0200091550792",
            price = Decimal.parse(price),
            measureUnitCode = null,
            vatGroup = null
        ),
        resultCode = 0,
        resultText = null
    )

    /** Справочник ответил, что товара нет, или не ответил вовсе (`-1`). */
    fun missing(resultCode: Int = 0) = NomenclatureLookupResponse(false, null, resultCode, "not found")

    fun position(price: String, quantity: String = "1", discount: String = "0") = Position(
        name = "Товар",
        price = tenge(price),
        quantity = decimal(quantity),
        vatGroup = "NO_VAT",
        discount = tenge(discount)
    )

    const val BARCODE = "5449000176431"

    /** Рабочее место окна снимка: открытая смена и язык сочетания. */
    internal fun window(): KassaDesk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))

    /** Касса такси с открытой сменой и пятью видами оплаты. */
    fun taxi(basket: Basket, form: SaleForm) = SaleUiState(
        kkm = CoreScene.kkm(),
        signedIn = true,
        shiftOpen = true,
        domainKind = DomainKind.Taxi,
        paymentTypes = KassaExtremes.PAYMENTS,
        basket = basket,
        form = form
    )
}
