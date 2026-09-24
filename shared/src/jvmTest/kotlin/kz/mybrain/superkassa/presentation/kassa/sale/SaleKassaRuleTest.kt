package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.payment.PaymentSplit
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.texport.superkassa.core.domain.api.model.common.Money as KassaMoney

/**
 * Сумма строки, итог и оплаты на экране — те же, что посчитает касса.
 *
 * Экран усекал строку до тиына и складывал усечённое, а касса округляет
 * строку к ближайшему: 1,5 кг по 333,33 ₸ экран называл 499,99, касса —
 * 500,00, и чек отвергался несходящейся оплатой. Правило строки берётся
 * у самой кассы, и здесь проверяется, что итог и оплата с ним сходятся.
 */
class SaleKassaRuleTest {

    private fun state(basket: Basket, form: SaleForm = SaleForm()) =
        SaleUiState(kkm = CoreScene.kkm(), signedIn = true, shiftOpen = true, basket = basket, form = form)

    private fun paid(state: SaleUiState): Long =
        state.receipt.command("kkm-1", "1234").payments.sumOf { Tenge.of(it.sum) }

    @Test
    fun `полтора килограмма по 333,33 — это 500,00, и оплата та же`() {
        val basket = Basket().add(SaleScene.position("333.33", quantity = "1.5"))
        val state = state(basket)

        assertEquals(tenge("500.00"), basket.positions.single().lineSum)
        assertEquals(tenge("500.00"), state.total)
        assertEquals(tenge("500.00"), paid(state), "оплата разошлась с итогом")
        val kassa = KassaMoney.lineSum(Decimal.parse("333.33"), Decimal.parse("1.5"))
        assertEquals(50_000L, kassa.tiyn(), "касса считает строку иначе, чем экран")
    }

    /** Пятьдесят строк с половиной тиына: усечение теряло по тиыну на строке. */
    @Test
    fun `пятьдесят дробных строк складываются так же, как у кассы`() {
        val basket = (1..50).fold(Basket()) { all, _ -> all.add(SaleScene.position("3.33", quantity = "0.150")) }
        val state = state(basket)

        assertEquals(tenge("25.00"), state.total, "экран усекал 0,4995 до 0,49 на каждой строке")
        assertEquals(state.total, paid(state))
    }

    @Test
    fun `процент скидки на чек и смешанная оплата сходятся с итогом`() {
        val basket = Basket().add(SaleScene.position("333.33", quantity = "1.5"))
        val form = SaleForm(split = PaymentSplit().add("CARD").enter(1, "123.45"))
            .switchDiscount(AdjustmentUnit.Percent).enterDiscount("10")
        val state = state(basket, form)
        val command = state.receipt.command("kkm-1", "1234")

        assertEquals(tenge("450.00"), state.total)
        assertEquals(tenge("50.00"), command.discountSum?.let(Tenge::of))
        assertEquals(tenge("450.00"), paid(state))
        assertEquals(listOf("CASH", "CARD"), command.payments.map { it.type })
    }
}
