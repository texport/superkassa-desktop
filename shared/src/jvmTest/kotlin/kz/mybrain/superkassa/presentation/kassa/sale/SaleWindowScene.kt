package kz.mybrain.superkassa.presentation.kassa.sale

import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.SaleScene

/*
 * Продажа в окне каркаса для снимков: рабочее место и касса такси.
 */

/** Рабочее место окна снимка: открытая смена и язык сочетания. */
internal fun SaleScene.window(): KassaDesk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))

/** Касса такси с открытой сменой и пятью видами оплаты. */
internal fun SaleScene.taxi(basket: Basket, form: SaleForm) = SaleUiState(
    kkm = CoreScene.kkm(),
    signedIn = true,
    shiftOpen = true,
    domainKind = DomainKind.Taxi,
    paymentTypes = KassaExtremes.PAYMENTS,
    basket = basket,
    form = form
)
