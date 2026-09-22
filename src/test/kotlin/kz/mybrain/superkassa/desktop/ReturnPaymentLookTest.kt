package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.returns.ReturnsScreen
import java.io.File
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
class ReturnPaymentLookTest {

    private fun sale() = Document(
        id = "sale-41",
        docNo = 41,
        printedDocumentNumber = 41,
        docType = "SALE",
        ofdStatus = "SENT",
        fiscalSign = "38000041",
        totalAmount = 1_137_250,
        createdAt = System.currentTimeMillis(),
        shiftNo = 7
    )

    private fun panel(folder: String, payments: List<DictionaryEntry>): ByteArray {
        val session = KassaScene.session(
            folder,
            shift = KassaScene.openShift(),
            journal = listOf(sale()),
            payments = payments
        )
        return RenderProbe(
            width = KassaScene.WIDE,
            height = KassaScene.TALL,
            content = { ReturnsScreen(session) }
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

    private fun entry(code: String, supported: Boolean = true) =
        DictionaryEntry(code = code, supported = supported)

    private companion object {
        const val SETTLE = 40
        const val BASIS_X = 300f
        const val BASIS_Y = 230f
    }
}
