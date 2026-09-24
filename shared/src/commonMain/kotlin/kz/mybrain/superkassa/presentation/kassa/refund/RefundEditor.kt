package kz.mybrain.superkassa.presentation.kassa.refund

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs

/**
 * Сумма и отметки возврата по выбранному чеку.
 *
 * Пока возврат оформляется, набранное не правится: в кассу уходит ровно
 * то, что кассир видел, нажимая кнопку.
 *
 * @param submit оформить возврат: это делает модель экрана.
 */
class RefundEditor(
    private val screen: MutableStateFlow<ReturnsUiState>,
    private val busy: Busy,
    private val submit: () -> Unit
) : RefundActions {

    override fun enter(amount: String) = edit { it.copy(entered = amount) }

    override fun wholeReceipt() = edit { it.whole() }

    override fun toggle(at: Int) = edit { it.toggle(at) }

    override fun contact(text: String) = edit { it.copy(contact = it.contact.enter(text)) }

    override fun contactKind(kind: ContactKind) = edit { it.copy(contact = it.contact.switchTo(kind)) }

    override fun refund() = submit()

    private fun edit(change: (RefundDraft) -> RefundDraft) =
        screen.update { now -> if (busy.now) now else now.copy(refund = now.refund?.let(change)) }
}

/**
 * Как названа единственная строка чека возврата суммой.
 *
 * Номер берётся тот, что стоит на бумаге покупателя: по номеру от БФД
 * покупатель свой чек не опознает.
 */
internal fun refundLineName(caption: String, draft: RefundDraft): String =
    "$caption ${draft.basis.number ?: Glyphs.DASH}"
