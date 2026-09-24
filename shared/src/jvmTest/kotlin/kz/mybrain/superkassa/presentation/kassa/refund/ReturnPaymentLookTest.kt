package kz.mybrain.superkassa.presentation.kassa.refund

import androidx.compose.ui.geometry.Offset
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Виды оплаты возврата: погасший вид объясняется словами.
 *
 * Узел объявляет допустимость каждого вида полем `supported`, и
 * непринимаемый вид не прячется, а гаснет в списке. Погасшая строка
 * без объяснения читается как поломка кассы — на продаже причина
 * написана, а возврат о ней молчал.
 *
 * Снимки — `/tmp/audit-returns-refund-payments-*.png`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReturnPaymentLookTest {

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun panel(folder: String, payments: List<PaymentTypeResponse>): ByteArray {
        val model = ReturnsScene.model(listOf(ReturnsScene.sale(41, 1_137_250)), payments = payments)
        return RenderProbe(
            width = KassaScene.WIDE,
            height = KassaScene.TALL,
            content = { ReturnsScreen(model) }
        ).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(BASIS_X, BASIS_Y))
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            File("/tmp/audit-returns-refund-payments-$folder.png").writeBytes(frame)
            frame
        }
    }

    @Test
    fun `непринимаемый вид оплаты возврата объяснён словами`() {
        val all = panel("all-supported", listOf(entry("CASH"), entry("CARD")))
        val refused = panel("with-refused", listOf(entry("CASH"), entry("CARD"), entry("CREDIT", supported = false)))

        assertTrue(
            !all.contentEquals(refused),
            "погасший вид оплаты на возврате ничем не объяснён: кассир читает его как поломку кассы"
        )
    }

    private fun entry(code: String, supported: Boolean = true) = ReturnsScene.payment(code, supported)

    private companion object {
        const val SETTLE = 40
        const val BASIS_X = 300f
        const val BASIS_Y = 230f
    }
}
