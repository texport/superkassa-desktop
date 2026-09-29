package kz.mybrain.superkassa.presentation.kassa.sale

import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.VatScope
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainInput
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleOperation
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions

/** Что кассир делает со строками чека. По умолчанию пусто — для снимков вида. */
interface BasketActions {
    fun storno(at: Int) = Unit

    fun remove(at: Int) = Unit

    fun stamp(at: Int, stamps: List<String>) = Unit

    fun clear() = Unit
}

/** Как позиция встаёт в чек: штрихкодом, руками или с ценой, спрошенной у кассира. */
interface EntryActions {
    fun typeBarcode(text: String) = Unit

    /** Ищет набранный код; `false` — искать нечего или поиск уже идёт. */
    fun search(): Boolean = false

    fun editDraft(draft: PositionDraft) = Unit

    /** Ставит набранную руками позицию в чек; `false` — она ещё не готова. */
    fun addDraft(): Boolean = false

    /** Позиция без цены получила цену и количество от кассира. */
    fun addPriced(position: Position) = Unit

    fun dismissPriced() = Unit
}

/** Что кассир набирает поверх корзины. */
interface FormActions {
    /** Выбор НДС чека: решение о чеке целиком, как скидка и наценка. */
    val vat: VatActions get() = object : VatActions {}

    fun operation(operation: SaleOperation) = Unit

    fun discount(text: String) = Unit

    fun discountUnit(unit: AdjustmentUnit) = Unit

    fun markup(text: String) = Unit

    fun markupUnit(unit: AdjustmentUnit) = Unit

    fun taken(text: String) = Unit

    /** Контакт покупателя: по нему ему уходит чек. */
    val contact: ContactActions get() = object : ContactActions {}

    fun customerBin(text: String) = Unit

    fun domain(input: DomainInput) = Unit
}

/** Контакт покупателя: его вид и сам контакт. */
interface ContactActions {
    fun kind(kind: ContactKind) = Unit

    fun text(text: String) = Unit
}

/** Как задан НДС чека: способ и ставка на весь чек. */
interface VatActions {
    fun scope(scope: VatScope) = Unit

    fun rate(rate: String) = Unit
}

/**
 * Все действия экрана продажи.
 *
 * Разложены по смыслу, а не одним списком в два десятка: чек, ввод
 * позиции, набранное поверх чека и оплата — разные части экрана, и каждая
 * получает только свои.
 */
class SaleActions(
    val issue: () -> Unit = {},
    val next: () -> Unit = {},
    val toggle: (SalePanel) -> Unit = {},
    val basket: BasketActions = object : BasketActions {},
    val entry: EntryActions = object : EntryActions {},
    val form: FormActions = object : FormActions {},
    val payments: PaymentActions = object : PaymentActions {}
)

/** Действия экрана, выполняемые этой моделью. */
internal fun SaleViewModel.actions(): SaleActions =
    SaleActions(::issue, ::nextReceipt, ::togglePanel, basket, entry, form, payments)
