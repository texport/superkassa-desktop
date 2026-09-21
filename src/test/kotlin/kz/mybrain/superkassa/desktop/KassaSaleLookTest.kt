package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.MessageEffect
import kz.mybrain.superkassa.desktop.ui.MessageHost
import kz.mybrain.superkassa.desktop.ui.sale.Basket
import kz.mybrain.superkassa.desktop.ui.sale.BasketCard
import kz.mybrain.superkassa.desktop.ui.sale.IssueRow
import kz.mybrain.superkassa.desktop.ui.sale.LocalSaleTexts
import kz.mybrain.superkassa.desktop.ui.sale.LocalUnits
import kz.mybrain.superkassa.desktop.ui.sale.LocalVatRates
import kz.mybrain.superkassa.desktop.ui.sale.Position
import kz.mybrain.superkassa.desktop.ui.sale.ReceiptTotals
import kz.mybrain.superkassa.desktop.ui.sale.SaleForm
import kz.mybrain.superkassa.desktop.ui.sale.SaleScreen
import kz.mybrain.superkassa.desktop.ui.sale.amount
import kz.mybrain.superkassa.desktop.ui.sale.vatRatesOf
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal
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
            "empty" to KassaScene.shot("sale-empty-basket") {
                SaleScreen(KassaScene.session("sale-empty", shift = KassaScene.openShift()))
            },
            "shift-closed" to KassaScene.shot("sale-shift-closed") {
                SaleScreen(KassaScene.session("sale-closed"))
            },
            "kkm-blocked" to KassaScene.shot("sale-kkm-blocked") {
                SaleScreen(
                    KassaScene.session(
                        "sale-blocked",
                        kkm = KassaScene.kkm(state = "BLOCKED"),
                        shift = KassaScene.openShift()
                    )
                )
            },
            "no-vat" to KassaScene.shot("sale-no-vat-regime") {
                SaleScreen(
                    KassaScene.session(
                        "sale-novat",
                        kkm = KassaScene.kkm(taxRegime = "NO_VAT"),
                        shift = KassaScene.openShift()
                    )
                )
            },
            "node-refuses" to KassaScene.shot("sale-node-refuses") {
                val session = KassaScene.session("sale-refuse", shift = KassaScene.openShift())
                session.lastMessage = Message.Refusal("Смена открыта больше суток, закройте её", "SHIFT_EXPIRED")
                val host = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { MessageHost(host) }) {
                    MessageEffect(session.lastMessage, host) {}
                    SaleScreen(session)
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
     * Корзину экран продажи держит в себе, поэтому набранный чек рисуется
     * его же частями: лист, итоги и кнопка — те самые, что стоят в окне.
     */
    @Composable
    private fun Receipt(session: Session, basket: Basket, form: SaleForm) {
        val total = basket.totalWith(amount(form.discount).value, amount(form.markup).value)
        CompositionLocalProvider(
            LocalSaleTexts provides saleTexts(session.language),
            LocalVatRates provides vatRatesOf(session, LocalStrings.current.enums),
            LocalUnits provides session.units
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(Spacing.screen),
                horizontalArrangement = Arrangement.spacedBy(Spacing.roomy)
            ) {
                BasketCard(basket, Modifier.weight(1f), {}, {}, {})
                Column(modifier = Modifier.width(TILL), verticalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                    ReceiptTotals(session, basket, form, total, expanded = true, onToggle = {})
                    IssueRow(session, basket, form)
                }
            }
        }
    }

    @Test
    fun `набранный чек рисуется вместе с итогом и помехами`() {
        val session = KassaScene.session("sale-basket", shift = KassaScene.openShift())
        val basket = Basket().apply { POSITIONS.forEach { add(it) } }

        val plain = KassaScene.shot("sale-basket") { Receipt(session, basket, SaleForm()) }
        val storno = KassaScene.shot("sale-basket-storno") {
            Receipt(session, Basket().apply { POSITIONS.forEach { add(it) }; stornoAt(1) }, SaleForm())
        }
        val overDiscount = KassaScene.shot("sale-discount-over-total") {
            Receipt(session, basket, SaleForm().apply { enterDiscount("999999") })
        }
        val shortTaken = KassaScene.shot("sale-taken-too-small") {
            Receipt(session, basket, SaleForm().apply { taken = "100" })
        }
        val change = KassaScene.shot("sale-change") {
            Receipt(session, basket, SaleForm().apply { taken = "20000" })
        }
        val badBin = KassaScene.shot("sale-bin-too-short") {
            Receipt(session, basket, SaleForm().apply { customerBin = "1234" })
        }
        // Вид оплаты, которого узел не принимает: он выбран, а не просто
        // погашен в списке — иначе на экране не видно ровно ничего.
        val unsupported = KassaScene.shot("sale-payment-unsupported") {
            val picky = KassaScene.session("sale-payment", shift = KassaScene.openShift(), payments = PAYMENTS)
            val form = SaleForm()
            form.split.retype(form.split.entries.first(), "CARD")
            Receipt(picky, basket, form)
        }
        val mixed = KassaScene.shot("sale-payment-mixed") {
            val form = SaleForm()
            form.split.add("CARD")
            form.split.entries.first().amount = "5000"
            Receipt(session, basket, form)
        }

        val frames = listOf(plain, storno, overDiscount, shortTaken, change, badBin, unsupported, mixed)
        frames.forEach { assertTrue(it.isNotEmpty()) }
        assertTrue(frames.map { it.toList() }.distinct().size == frames.size, "состояния чека неотличимы")
    }

    private companion object {
        /** Ширина кассовой колонки на снимке: та же, что в окне кассира. */
        val TILL = Sizes.fieldForm + Sizes.fieldPrice + Sizes.fieldQuantity

        /** Виды оплаты узла, где кредит и тара объявлены непринимаемыми. */
        val PAYMENTS = listOf(
            DictionaryEntry("CASH", mapOf("ru" to "Наличные")),
            DictionaryEntry("CARD", mapOf("ru" to "Карта"), supported = false),
            DictionaryEntry("MOBILE", mapOf("ru" to "Мобильный платёж"))
        )

        /** Позиции для листа чека: дробное количество, акциз и длинное имя. */
        val POSITIONS = listOf(
            Position(
                name = "Баранина на косточке, охлаждённая",
                price = BigDecimal("3450.00"),
                quantity = BigDecimal("1.450"),
                vatGroup = "VAT_16",
                measureUnitCode = "166"
            ),
            Position(
                name = "Вода питьевая негазированная «Тау Самалы» 5 л в упаковке по шесть бутылок",
                price = BigDecimal("690.00"),
                quantity = BigDecimal("2"),
                vatGroup = "VAT_16",
                measureUnitCode = "796"
            ),
            Position(
                name = "Коньяк «Казахстан» 0,5 л",
                price = BigDecimal("4990.00"),
                quantity = BigDecimal("1"),
                vatGroup = "VAT_16",
                measureUnitCode = "796",
                exciseStamps = listOf("AB1234567890")
            )
        )
    }
}
