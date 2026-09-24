package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.VatScope
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.model.payment.unsupportedPayments
import kz.mybrain.superkassa.domain.kassa.model.paysVat
import kz.mybrain.superkassa.domain.kkm.model.isBlocked

/**
 * Чек продажи, как его набрал кассир: всё, из чего собирается команда кассе.
 *
 * @property kkm касса, за которой пробивают: её режим НДС и ставка по умолчанию.
 * @property vatRates справочник ставок кассы; пусто — не прочитан.
 * @property key ключ повтора этого чека. Не меняется, пока чек не принят:
 *   если ответ потерялся, повтор с тем же ключом не пробьёт второй чек.
 */
data class SaleReceipt(
    val basket: Basket = Basket(),
    val form: SaleForm = SaleForm(),
    val domainKind: DomainKind = DomainKind.Trading,
    val kkm: KkmResponse? = null,
    val vatRates: List<VatRateResponse> = emptyList(),
    val key: String = ""
) {
    /** Итог чека со скидкой или наценкой на него, в тиынах: его назовут покупателю. */
    val total: Long get() = totalOf(basket, form)

    /** Плательщик ли касса НДС: только у него есть выбор «на весь чек или по позициям». */
    val vatPayer: Boolean get() = paysVat(kkm)

    /** Ставка кассы по умолчанию — в пределах справочника и режима. */
    val kassaVat: String get() = defaultVatOf(kkm, vatRates)

    /** НДС этого чека задан на весь чек, а не по позициям. */
    val vatOnReceipt: Boolean get() = form.vat.scopeAt(vatPayer) == VatScope.Receipt

    /** Ставка на весь чек, как она уйдёт в чек; `null` — НДС по позициям. */
    val receiptVat: String? get() = form.vat.receiptRate(vatPayer, kassaVat)

    /**
     * Снимок для правил: от него зависит, можно ли пробить чек.
     *
     * @param signedIn кассир вошёл и пин принят: без пина чек не пробить.
     * @param shiftOpen смена открыта со слов кассы.
     * @param paymentTypes виды оплаты из справочника кассы; пусто — не прочитан.
     */
    fun state(signedIn: Boolean, shiftOpen: Boolean, paymentTypes: List<PaymentTypeResponse>): SaleState {
        val total = total
        return SaleState(
            hasKkm = kkm != null,
            kkmBlocked = kkm?.isBlocked == true,
            hasPin = signedIn,
            shiftOpen = shiftOpen,
            positions = basket.positions.size,
            hasItemDiscount = basket.hasItemDiscount,
            hasZeroLine = basket.hasZeroLine,
            discount = form.discount,
            markup = form.markup,
            itemsSum = basket.total,
            total = total,
            paymentCodes = form.split.types,
            splitIssue = form.split.issue(total),
            unsupportedPayments = unsupportedPayments(paymentTypes),
            taken = amount(form.taken).tiyn,
            cashSum = form.split.cashSum(total),
            customerBin = form.customerBin,
            contactMalformed = form.contact.malformed,
            missingDomainField = form.domain.missing(domainKind)
        )
    }

    /** Ставки НДС, которые уйдут в кассу: у строк и на весь чек. */
    val vatCodes: List<String>
        get() = basket.toItems { own -> form.vat.positionRate(vatPayer, own) }.mapNotNull { it.vatGroup } +
            listOfNotNull(receiptVat)

    /**
     * Чек для кассы.
     *
     * Скидка и наценка уходят суммой в тенге, даже когда кассир набрал их
     * процентом: сумма посчитана тем же правилом, что у кассы, и «Итого»
     * на экране совпадает с чеком до тиына. Нулевая скидка не уходит вовсе:
     * касса сочла бы её скидкой на чек и отказала бы чеку со скидкой
     * на позиции.
     *
     * «Принято» относится только к наличным: касса сверяет принятое
     * с наличной частью чека.
     *
     * НДС уходит одним способом из двух — его выбирает `ReceiptVat`: на весь
     * чек — ставкой чека, а строки без своих ставок; по позициям — ставкой
     * у каждой строки, а ставки чека в запросе нет. Касса не принимает оба
     * способа сразу, как не принимает скидку на позицию вместе со скидкой на чек.
     */
    fun command(kkmId: String, pin: String): CreateReceiptCommand {
        val itemsSum = basket.total
        val total = total
        return CreateReceiptCommand(
            kkmId = kkmId,
            pin = pin,
            operation = form.operation.type.name,
            idempotencyKey = key,
            items = basket.toItems { own -> form.vat.positionRate(vatPayer, own) },
            discountPercent = null,
            discountSum = form.discount.sumOf(itemsSum)?.takeIf { it > 0L }?.let(Tenge::decimal),
            markupPercent = null,
            markupSum = form.markup.sumOf(itemsSum)?.takeIf { it > 0L }?.let(Tenge::decimal),
            payments = form.split.toPayments(total),
            taken = amount(form.taken).value?.takeIf { form.split.cashSum(total) > 0L },
            domain = form.domain.toDomain(domainKind),
            customerBin = form.customerBin.takeIf { it.isNotBlank() },
            customerContact = form.contact.toRequest(),
            vatGroup = receiptVat
        )
    }
}
