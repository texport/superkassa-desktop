package kz.mybrain.superkassa.presentation.kassa.refund

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions

/** Поиск чека-основания. По умолчанию пусто — для снимков вида. */
interface BasisActions {
    fun kind(kind: ReturnKind) = Unit

    fun day(day: LocalDate) = Unit

    fun number(text: String) = Unit

    fun choose(basis: FiscalDocumentResponse) = Unit

    /** Назад к списку: на узком окне панель стоит вместо него. */
    fun back() = Unit

    fun rereadDay() = Unit
}

/** Возврат по выбранному чеку. */
interface RefundActions {
    fun enter(amount: String) = Unit

    /** Сумма — весь чек, отметки снимаются. */
    fun wholeReceipt() = Unit

    fun toggle(at: Int) = Unit

    /** Контакт покупателя: по нему ему уходит чек возврата. */
    fun contact(text: String) = Unit

    fun contactKind(kind: ContactKind) = Unit

    /** Спросить подтверждение возврата: сумма и чек-основание. */
    fun refund() = Unit

    /** Подтверждено: возврат уходит в кассу. */
    fun confirm() = Unit

    /** Не возвращать: вопрос закрыт, набранное остаётся. */
    fun cancel() = Unit

    /** Чек оформленного возврата показан — кассир берётся за следующий. */
    fun next() = Unit
}

/** Все действия экрана возврата, разложенные по частям экрана. */
class ReturnsActions(
    val basis: BasisActions = object : BasisActions {},
    val refund: RefundActions = object : RefundActions {},
    val payments: PaymentActions = object : PaymentActions {}
)
