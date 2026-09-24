package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.Fiscal
import kz.mybrain.superkassa.domain.kassa.model.entry.amount

/**
 * Пробитый чек, как его называют покупателю: сумма и сдача.
 *
 * Корзина после чека пуста, и без этого итога кассиру оставалась одна
 * строка сообщений: сумму и сдачу приходилось держать в уме, а чек —
 * искать в журнале, чтобы показать или распечатать.
 *
 * @property documentId документ кассы: по нему чек показывают и печатают.
 * @property total итог чека в тиынах.
 * @property change сдача покупателю в тиынах; `null` — наличных не принимали
 *   или принятое не набирали.
 */
data class IssuedReceipt(
    val documentId: String,
    val operation: SaleOperation,
    val total: Long,
    val change: Long?
)

/** Итог этого чека по ответу кассы: сумма и сдача с набранного «принято». */
fun SaleReceipt.issued(fiscal: Fiscal): IssuedReceipt = IssuedReceipt(
    documentId = fiscal.documentId,
    operation = form.operation,
    total = total,
    change = amount(form.taken).tiyn?.let { changeOf(it, form.split.cashSum(total)) }
)
