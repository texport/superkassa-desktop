package kz.mybrain.superkassa.presentation.kassa.sale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.VatScope
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainInput
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleOperation

/**
 * Правка строк чека.
 *
 * Пока чек пробивается, корзина не правится: пробивается ровно то,
 * что кассир видел, нажимая кнопку.
 */
class BasketEditor(private val screen: MutableStateFlow<SaleUiState>) : BasketActions {
    override fun storno(at: Int) = edit { it.stornoAt(at) }

    override fun remove(at: Int) = edit { it.removeAt(at) }

    override fun stamp(at: Int, stamps: List<String>) = edit { it.stampAt(at, stamps) }

    override fun clear() = edit { Basket() }

    private fun edit(change: (Basket) -> Basket) =
        screen.update { if (it.issuing) it else it.copy(basket = change(it.basket)) }
}

/** Правка набранного поверх корзины. */
class FormEditor(private val screen: MutableStateFlow<SaleUiState>) : FormActions {
    override val vat: VatActions = VatEditor(screen)

    override fun operation(operation: SaleOperation) = edit { it.copy(operation = operation) }

    override fun discount(text: String) = edit { it.enterDiscount(text) }

    override fun discountUnit(unit: AdjustmentUnit) = edit { it.switchDiscount(unit) }

    override fun markup(text: String) = edit { it.enterMarkup(text) }

    override fun markupUnit(unit: AdjustmentUnit) = edit { it.switchMarkup(unit) }

    override fun taken(text: String) = edit { it.copy(taken = text) }

    override val contact: ContactActions = ContactEditor(screen)

    override fun customerBin(text: String) = edit { it.enterBin(text) }

    override fun domain(input: DomainInput) = edit { it.copy(domain = input) }

    private fun edit(change: (SaleForm) -> SaleForm) =
        screen.update { if (it.issuing) it else it.copy(form = change(it.form)) }
}

/** Контакт покупателя: вид и набранное. */
class ContactEditor(private val screen: MutableStateFlow<SaleUiState>) : ContactActions {
    override fun kind(kind: ContactKind) = screen.update { if (it.issuing) it else it.chooseContact(kind) }

    override fun text(text: String) = edit { it.enterContact(text) }

    private fun edit(change: (SaleForm) -> SaleForm) =
        screen.update { if (it.issuing) it else it.copy(form = change(it.form)) }
}

/** Выбор НДС чека: на весь чек или по позициям, и ставка чека. */
class VatEditor(private val screen: MutableStateFlow<SaleUiState>) : VatActions {
    override fun scope(scope: VatScope) = edit { it.switchVat(scope) }

    override fun rate(rate: String) = edit { it.chooseVat(rate) }

    private fun edit(change: (SaleForm) -> SaleForm) =
        screen.update { if (it.issuing) it else it.copy(form = change(it.form)) }
}
