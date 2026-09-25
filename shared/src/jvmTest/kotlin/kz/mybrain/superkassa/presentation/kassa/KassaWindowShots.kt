package kz.mybrain.superkassa.presentation.kassa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.IssuedReceipt
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleOperation
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.ReturnsScene
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsContent
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsUiState
import kz.mybrain.superkassa.presentation.kassa.sale.ReceiptOutput
import kz.mybrain.superkassa.presentation.kassa.sale.SaleContent
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.shell.frame.ShellFrame
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.sectionFrame
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Продажа и возврат в окне кассы — с рельсом разделов, как их ставит каркас.
 *
 * Кадры смотрит человек: занимает ли рабочая область окно, выделен ли
 * блок оплаты, виден ли ручной ввод позиции. Кадры —
 * `/tmp/kassa-window-<экран>-<окно>.png`.
 */
class KassaWindowShots {

    private val basket = listOf("450", "1200", "85.50")
        .fold(Basket()) { all, price -> all.add(SaleScene.position(price)) }

    private val sale = SaleUiState(
        kkm = CoreScene.kkm(),
        signedIn = true,
        shiftOpen = true,
        basket = basket,
        channels = ContactChannels(setOf(ContactKind.Phone))
    )

    /** Чек только что пробит: корзина следующего пуста, наличными дали 2 000 ₸. */
    private val issued = sale.copy(
        basket = Basket(),
        issued = IssuedReceipt("doc-1", SaleOperation.Sell, total = 173_550, change = 26_450)
    )

    private val basis = ReturnsScene.sale(42, 90_000)

    private val returns = ReturnsUiState(
        kkm = CoreScene.kkm(),
        signedIn = true,
        shiftOpen = true,
        loading = false,
        dayRead = true,
        documents = listOf(basis, ReturnsScene.sale(43, 15_000)),
        refund = RefundDraft(basis, itemsRead = true)
    )

    @Test
    fun `продажа и возврат в окне кассы`() {
        SIZES.forEach { (width, height) ->
            shoot("sale", width, height, Section.Sale) { SaleContent(sale) }
            shoot("sale-empty", width, height, Section.Sale) { SaleContent(sale.copy(basket = Basket())) }
            shoot("sale-issued", width, height, Section.Sale) { SaleContent(issued, output = OUTPUT) }
            // Чек выбран: на узком окне он открыт поверх списка шагом истории.
            shoot("returns", width, height, Section.Returns) { ReturnsContent(returns, stepped = true) }
            shoot("returns-confirm", width, height, Section.Returns) {
                ReturnsContent(returns.copy(confirming = true), stepped = true)
            }
        }
    }

    private fun shoot(name: String, width: Int, height: Int, section: Section, content: @Composable () -> Unit) {
        val file = File("/tmp/kassa-window-$name-${width}x$height.png")
        RenderProbe(width, height) {
            Surface(Modifier.fillMaxSize()) {
                ShellFrame(Section.entries, section, {}, topBar = {}) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).sectionFrame().fillMaxHeight()) { content() }
                }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            file.writeBytes(probe.frame())
        }
        assertTrue(file.length() > 0, "кадр ${file.name} пуст")
    }

    private companion object {
        const val SETTLE = 20
        const val VERSION = "1.0.6"

        /** Показ и печать, как их подаёт каркас окна. */
        val OUTPUT = ReceiptOutput(show = {}, print = {})

        /** Планшет лёжа, настольное окно и большой планшет. */
        val SIZES = listOf(1280 to 800, 1920 to 1080, 2560 to 1600)
    }
}
