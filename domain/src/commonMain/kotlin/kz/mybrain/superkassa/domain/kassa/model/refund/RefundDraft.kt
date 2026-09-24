package kz.mybrain.superkassa.domain.kassa.model.refund

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptDomainRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kz.mybrain.superkassa.domain.kassa.model.BuyerContact
import kz.mybrain.superkassa.domain.kassa.model.FiscalOutcome
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.attemptKey
import kz.mybrain.superkassa.domain.kassa.model.payment.PaymentSplit

/**
 * Возврат по одному чеку-основанию: сумма, оплаты и отмеченные строки.
 *
 * @property entered сумма возврата, как её набрал кассир.
 * @property items строки чека-основания; пусто — касса их ещё не отдала.
 * @property itemsRead касса отдала строки; `false` — не отдала, и строки
 *   не показываются вовсе.
 * @property chosen отмеченные строки: их доля и есть сумма возврата.
 * @property attempt последняя попытка этого возврата: повтор того же
 *   возврата после неизвестного исхода идёт с её ключом.
 * @property contact контакт покупателя: по нему ему уходит чек возврата.
 */
data class RefundDraft(
    val basis: FiscalDocumentResponse,
    val entered: String = Tenge.entered(basis.totalAmount ?: 0L),
    val split: PaymentSplit = PaymentSplit(),
    val items: List<ReceiptItemView> = emptyList(),
    val itemsRead: Boolean = false,
    val chosen: Set<Int> = emptySet(),
    val attempt: RefundAttempt? = null,
    val contact: BuyerContact = BuyerContact()
) {
    /** Итог чека-основания в тиынах. */
    val total: Long get() = basis.totalAmount ?: 0L

    val checked: RefundAmount get() = refundAmountOf(entered, total)

    /** Принятая сумма возврата в тиынах, а до её принятия — ноль: от неё разбиваются оплаты. */
    val readyTiyn: Long get() = (checked as? RefundAmount.Ready)?.tiyn ?: 0L

    /** Доля итога чека-основания на каждую строку. */
    val shares: List<Long> get() = refundShares(items, total)

    /** Сумма отмеченных строк в тиынах: столько и вернётся покупателю. */
    val chosenTiyn: Long get() = chosen.sumOf { shares.getOrElse(it) { 0L } }

    /**
     * Отметки описывают чек возврата, только пока сумма осталась их суммой:
     * поправленное поле отправит одну строку на набранную сумму.
     */
    val byLines: Boolean get() = chosen.isNotEmpty() && chosenTiyn == readyTiyn

    val ready: Boolean get() = checked is RefundAmount.Ready && split.issue(readyTiyn) == null && !contact.malformed

    /** Отмечает строку или снимает отметку; сумма становится суммой отмеченных. */
    fun toggle(at: Int): RefundDraft {
        val now = if (at in chosen) chosen - at else chosen + at
        return copy(chosen = now, entered = Tenge.entered(if (now.isEmpty()) total else now.sumOf { shares[it] }))
    }

    /** Сумма — весь чек, отметки снимаются. */
    fun whole(): RefundDraft = copy(chosen = emptySet(), entered = Tenge.entered(total))

    /**
     * Что уйдёт в кассу чеком возврата.
     *
     * @param lineName как назвать единственную строку возврата суммой.
     * @param domain отрасль кассы без подблока.
     */
    fun plan(kind: ReturnKind, kgdKkmId: String, lineName: String, domain: ReceiptDomainRequest) = RefundPlan(
        kind = kind,
        basis = basis,
        kgdKkmId = kgdKkmId,
        refundTiyn = readyTiyn,
        lines = if (byLines) chosen.sorted().map { items[it] to shares[it] } else emptyList(),
        lineName = lineName,
        payments = split.toPayments(readyTiyn),
        domain = domain,
        contact = contact.toRequest()
    )

    /** Ключ живёт, пока возвращают то же самое; другое — другой возврат со своим ключом. */
    fun attemptFor(plan: RefundPlan): RefundAttempt {
        val content = plan.toString()
        return attempt?.takeIf { it.content == content } ?: RefundAttempt(content, attemptKey(KEY_KIND))
    }

    /**
     * Возврат после ответа кассы; `null` — он проведён, и выбор снимается:
     * сумма чека в поле после частичного возврата приглашала бы вернуть его
     * ещё раз. Отвергнутый БФД остаётся для исправления без прежнего ключа.
     */
    fun after(outcome: FiscalOutcome): RefundDraft? = when (outcome) {
        FiscalOutcome.Accepted -> null
        FiscalOutcome.Rejected -> copy(attempt = null)
        FiscalOutcome.Unsettled -> this
    }
}

/**
 * Попытка возврата: что именно отправлено и с каким ключом.
 *
 * Ключ живёт, пока возвращают то же самое: та же сумма, те же строки,
 * те же оплаты. Поправленная после сбоя сумма — уже другой возврат и идёт
 * со своим ключом: с прежним касса ответила бы прежним документом,
 * и кассир счёл бы проведённой сумму, которой не было.
 */
data class RefundAttempt(val content: String, val key: String)

private const val KEY_KIND = "return"
