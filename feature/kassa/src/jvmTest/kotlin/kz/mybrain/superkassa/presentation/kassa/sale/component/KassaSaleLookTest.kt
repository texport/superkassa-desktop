package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.payment.PaymentSplit
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.MessageEffect
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.presentation.kassa.sale.SaleContent
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Экран продажи во всех состояниях, ради которых он и разбирается.
 *
 * Снимки — `/tmp/kassa-sale-*.png`. Проверяется не красота, а то, что
 * помеха названа словами и стоит под погашенной кнопкой: серая кнопка
 * «Пробить чек» без строки под ней — самая дорогая ошибка кассового
 * экрана, кассир не знает, что исправлять.
 */
class KassaSaleLookTest {

    @Test
    fun `экран продажи собирается во всех отказных состояниях и они различимы`() {
        val frames = mapOf(
            "empty" to KassaScene.shot("sale-empty-basket", height = FORM_TALL) { SaleContent(open()) },
            "shift-closed" to KassaScene.shot("sale-shift-closed") { SaleContent(open().copy(shiftOpen = false)) },
            "kkm-blocked" to KassaScene.shot("sale-kkm-blocked") {
                SaleContent(open().copy(kkm = CoreScene.kkm(state = "BLOCKED")))
            },
            // Кадр выше остальных: у кассы без НДС в форме позиции нет
            // выбора ставки, и единица занимает строку одна. На обычной
            // высоте эта строка уходила под сгиб, и режим кассы на снимке
            // было не различить вовсе.
            "no-vat" to KassaScene.shot("sale-no-vat-regime", height = FORM_TALL) {
                SaleContent(open().copy(kkm = CoreScene.kkm().copy(taxRegime = "NO_VAT")))
            },
            "kassa-refuses" to KassaScene.shot("sale-kassa-refuses") {
                val refusal = Message.Refusal("Смена открыта больше суток, закройте её", "SHIFT_LONGER_THAN_DAY")
                val host = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { MessageHost(host) }) {
                    MessageEffect(refusal, host) {}
                    SaleContent(open())
                }
            }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "отказные состояния продажи неотличимы друг от друга"
        )
    }

    /**
     * Лист чека с набранными позициями и денежный блок под ним.
     *
     * Набранный чек рисуется частями экрана продажи: лист, итоги и кнопка — те самые, что стоят в окне.
     */
    @Composable
    private fun Receipt(state: SaleUiState) {
        Till(state) {
            Row(
                modifier = Modifier.fillMaxSize().padding(Spacing.fieldGap),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
            ) {
                BasketCard(state.basket, Modifier.weight(1f), {}, {}, {})
                Column(modifier = Modifier.width(TILL), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    ReceiptChangesCard(state, NO_FORM, expanded = true, onToggle = {})
                    PaymentCard(state, NO_PAYMENTS, expanded = true, onToggle = {}, onTaken = {})
                    ReceiptTotal(state)
                    IssueRow(state) {}
                }
            }
        }
    }

    /**
     * Блок скидок и наценок: набранное и его последствие.
     *
     * Снимок смотрят глазами ради одного: видно ли из блока, во что
     * обошлась скидка. Поэтому состояния различаются не полями, а теми
     * строками «было — стало», которые кассир называет покупателю.
     */
    @Test
    fun `блок скидок показывает было и стало`() {
        val basket = POSITIONS.fold(Basket(), Basket::add)
        val discounted = Basket(listOf(POSITIONS[0].copy(discount = tenge("500.00"))) + POSITIONS.drop(1))

        val plain = KassaScene.shot("sale-changes-plain") { Changes(open(basket)) }
        val discount = KassaScene.shot(
            "sale-changes-discount"
        ) { Changes(open(basket, SaleForm().enterDiscount("1500"))) }
        val markup = KassaScene.shot("sale-changes-markup") { Changes(open(basket, SaleForm().enterMarkup("1500"))) }
        // Скидка по позициям названа в блоке строкой, и скидка на чек
        // рядом с ней краснеет: вместе их касса не принимает.
        val byLine = KassaScene.shot("sale-changes-by-line") {
            Changes(open(discounted, SaleForm().enterDiscount("1500")))
        }

        val byPercent = KassaScene.shot("sale-changes-percent") {
            Changes(open(basket, SaleForm().switchDiscount(AdjustmentUnit.Percent).enterDiscount("10")))
        }
        // Процент сверх ста касса не принимает: поле краснеет, а причина
        // названа под ним — до нажатия, а не после отказа.
        val overPercent = KassaScene.shot("sale-changes-percent-over") {
            Changes(open(basket, SaleForm().switchMarkup(AdjustmentUnit.Percent).enterMarkup("150")))
        }
        // Скидка, набранная не числом, не исчезает молча: поле краснеет,
        // а причина названа, как у всякой помехи.
        val notANumber = KassaScene.shot("sale-changes-not-a-number") {
            Changes(open(basket, SaleForm().enterDiscount("10,005")))
        }

        val frames = listOf(plain, discount, markup, byLine, byPercent, overPercent, notANumber)
        frames.forEach { assertTrue(it.isNotEmpty()) }
        assertTrue(frames.map { it.toList() }.distinct().size == frames.size, "состояния блока скидок неотличимы")
    }

    /** Один блок скидок в кассовой колонке — тот же, что стоит в окне. */
    @Composable
    private fun Changes(state: SaleUiState) {
        Till(state) {
            Column(modifier = Modifier.width(TILL).padding(Spacing.fieldGap)) {
                ReceiptChangesCard(state, NO_FORM, expanded = true, onToggle = {})
            }
        }
    }

    @Test
    fun `набранный чек рисуется вместе с итогом и помехами`() {
        val basket = POSITIONS.fold(Basket(), Basket::add)

        val plain = KassaScene.shot("sale-basket", height = RECEIPT_TALL) { Receipt(open(basket)) }
        val storno = KassaScene.shot("sale-basket-storno", height = RECEIPT_TALL) {
            Receipt(open(basket.stornoAt(1)))
        }
        val overDiscount = KassaScene.shot("sale-discount-over-total", height = RECEIPT_TALL) {
            Receipt(open(basket, SaleForm().enterDiscount("999999")))
        }
        val shortTaken = KassaScene.shot("sale-taken-too-small", height = RECEIPT_TALL) {
            Receipt(open(basket, SaleForm(taken = "100")))
        }
        val change = KassaScene.shot("sale-change", height = RECEIPT_TALL) {
            Receipt(open(basket, SaleForm(taken = "20000")))
        }
        val badBin = KassaScene.shot("sale-bin-too-short", height = RECEIPT_TALL) {
            Receipt(open(basket, SaleForm(customerBin = "1234")))
        }
        // Вид оплаты, которого касса не принимает: он выбран, а не просто
        // погашен в списке — иначе на экране не видно ровно ничего.
        val unsupported = KassaScene.shot("sale-payment-unsupported", height = RECEIPT_TALL) {
            val form = SaleForm(split = PaymentSplit().retype(0, "CARD"))
            Receipt(open(basket, form).copy(paymentTypes = PAYMENTS))
        }
        val mixed = KassaScene.shot("sale-payment-mixed", height = RECEIPT_TALL) {
            Receipt(open(basket, SaleForm(split = PaymentSplit().add("CARD").enter(0, "5000"))))
        }

        val frames = listOf(plain, storno, overDiscount, shortTaken, change, badBin, unsupported, mixed)
        frames.forEach { assertTrue(it.isNotEmpty()) }
        assertTrue(frames.map { it.toList() }.distinct().size == frames.size, "состояния чека неотличимы")
    }
}
