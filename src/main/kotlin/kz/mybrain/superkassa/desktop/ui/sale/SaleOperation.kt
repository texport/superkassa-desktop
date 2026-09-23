package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.server.FiscalResult
import kz.mybrain.superkassa.desktop.server.ReceiptDomain
import kz.mybrain.superkassa.desktop.server.ReceiptPayment
import kz.mybrain.superkassa.desktop.server.ReceiptRequest
import kz.mybrain.superkassa.desktop.server.buy
import kz.mybrain.superkassa.desktop.server.sell
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import java.math.BigDecimal

/**
 * Направление чека.
 *
 * Продажа кладёт деньги в кассу, покупка — выдаёт из неё. Названия взяты
 * из речи кассира, а не из протокола.
 */
enum class SaleOperation(
    val title: (SaleStrings) -> String,
    val action: (SaleStrings) -> String
) {
    Sell({ it.sale }, { it.issueSale }),
    Buy({ it.purchase }, { it.issuePurchase });

    suspend fun send(session: Session, kkmId: String, request: ReceiptRequest): FiscalResult = when (this) {
        Sell -> session.client.sell(kkmId, request, session.pin)
        Buy -> session.client.buy(kkmId, request, session.pin)
    }
}

/**
 * Всё, что кассир ввёл поверх корзины.
 *
 * Ключ повтора приходит извне и живёт до принятого чека: если ответ узла
 * потерялся в дороге, повтор с тем же ключом не пробьёт второй чек.
 */
data class ReceiptInput(
    val operation: SaleOperation,
    val payments: List<ReceiptPayment>,
    val cashSum: BigDecimal,
    val taken: BigDecimal?,
    val discount: BigDecimal?,
    val markup: BigDecimal?,
    val customerBin: String,
    /** Вид отрасли и её единственный подблок: протокол требует их у каждого чека. */
    val domain: ReceiptDomain,
    val idempotencyKey: String
) {
    /**
     * «Принято» относится только к наличным.
     *
     * Узел сверяет принятое с наличной частью чека; при оплате одной
     * картой непогашенное поле уехало бы в чек безо всякого смысла.
     */
    val cashTaken: BigDecimal? get() = taken.takeIf { cashSum.signum() > 0 }
}

/**
 * Оформляет чек и сообщает кассиру исход его словами.
 *
 * Очередь — не ошибка: чек пробит, фискальный эффект применён, а доставка
 * догонит. Путать это с отказом нельзя.
 */
suspend fun issueReceipt(
    session: Session,
    basket: Basket,
    input: ReceiptInput,
    texts: AppStrings,
    extra: SaleTexts,
    statusTitle: (String?) -> String
): Boolean {
    val kkm = session.selected ?: return false
    val request = ReceiptRequest(
        idempotencyKey = input.idempotencyKey,
        items = basket.toReceiptItems(),
        payments = input.payments,
        discountSum = input.discount,
        markupSum = input.markup,
        taken = input.cashTaken,
        customerBin = input.customerBin.takeIf { it.isNotBlank() },
        domain = input.domain,
        defaultVatGroup = kkm.defaultVatGroup
    )
    val title = input.operation.title(texts.sale)
    val result = session.guard(title) { input.operation.send(session, kkm.kkmId, request) }
    if (result == null) {
        retellRefusal(session, extra)
        return false
    }
    session.report("$title: ${outcome(result, texts, statusTitle)}")
    basket.clear()
    session.refreshSelected()
    return true
}

/** Исход доставки словами: доставлен, поставлен в очередь или иное состояние. */
private fun outcome(
    result: FiscalResult,
    texts: AppStrings,
    statusTitle: (String?) -> String
): String = when {
    result.isDelivered -> texts.sale.delivered
    result.isQueued -> texts.sale.queued
    else -> "${texts.sale.deliveryState} — ${statusTitle(result.deliveryStatus)}"
}

/**
 * Переписывает отказ узла словами кассира.
 *
 * Часть отказов узел отдаёт только по-английски и в терминах поля запроса:
 * «taken must be >= sum of CASH payments» кассиру не говорит ничего.
 * Незнакомый отказ остаётся как есть — придумывать за узел нельзя.
 */
private fun retellRefusal(session: Session, texts: SaleTexts) {
    val refusal = session.lastMessage as? Message.Refusal ?: return
    val retold = refusalWords(refusal.code, refusal.text, texts) ?: return
    session.lastMessage = Message.Refusal(retold, refusal.code)
}

/**
 * Отказ узла словами кассира или `null`, если такой отказ приложению незнаком.
 *
 * Коды сверены с ответами узла: он отвечает ими на закрытую смену, на вид
 * оплаты вне протокола 2.0.4, на скидку сразу двух уровней и на пустой
 * список позиций.
 */
fun refusalWords(code: String, detail: String, texts: SaleTexts): String? = when (code) {
    "SHIFT_NOT_OPEN" -> texts.blockShiftClosed
    "PAYMENT_TYPE_NOT_SUPPORTED" -> texts.blockPaymentUnsupported
    "RECEIPT_DISCOUNT_SCOPES_CONFLICT" -> texts.blockDiscountScopes
    "KKM_BLOCKED" -> texts.blockKkmBlocked
    "INSUFFICIENT_CASH" -> texts.refusalNoCash
    "VALIDATION_ERROR" -> texts.blockEmptyBasket.takeIf { detail.contains(ITEMS_FIELD) }
    "INVALID_ARGUMENT" -> byArgument(detail, texts)
    else -> null
}

private fun byArgument(detail: String, texts: SaleTexts): String? = when {
    detail.contains(TAKEN_FIELD) -> texts.blockTakenTooSmall
    detail.contains(VAT_FIELD) -> texts.refusalVatGroup
    else -> null
}

private const val ITEMS_FIELD = "items"
private const val TAKEN_FIELD = "taken"
private const val VAT_FIELD = "vatGroup"
