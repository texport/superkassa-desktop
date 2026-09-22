package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.ui.returns.ReturnsScreen
import java.io.File
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Возврат наличными из ящика, в котором столько денег нет.
 *
 * Деньги покупателю отдают из того же ящика, из которого их изымают:
 * изъятие сверх остатка касса не проводит, а возврат той же суммы
 * уходил молча. Кассир называл покупателю сумму, которой в ящике нет,
 * и узнавал об этом, уже открыв ящик.
 *
 * Снимки — `/tmp/audit-returns-refund-drawer-*.png`.
 */
class ReturnDrawerLookTest {

    private fun sale(tiyn: Long) = Document(
        id = "sale-41",
        docNo = 41,
        printedDocumentNumber = 41,
        docType = "SALE",
        ofdStatus = "SENT",
        fiscalSign = "38000041",
        totalAmount = tiyn,
        createdAt = System.currentTimeMillis(),
        shiftNo = 7
    )

    /** Панель возврата с выбранным чеком-основанием при заданном остатке ящика. */
    private fun panel(folder: String, drawerTiyn: Long): ByteArray {
        val session = KassaScene.session(
            folder,
            shift = KassaScene.openShift(),
            journal = listOf(sale(TOTAL)),
            sold = ITEMS,
            cashInDrawerTiyn = drawerTiyn
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
            File("/tmp/audit-returns-refund-drawer-$folder.png").writeBytes(frame)
            frame
        }
    }

    @Test
    fun `нехватка наличных в ящике видна до выдачи денег`() {
        val enough = panel("enough", TOTAL)
        val short = panel("short", TOTAL - 1)

        assertTrue(
            !enough.contentEquals(short),
            "панель возврата одинакова и при полном ящике, и при пустом: кассир открывает ящик за деньгами, которых нет"
        )
    }

    private companion object {
        const val SETTLE = 40
        const val BASIS_X = 300f
        const val BASIS_Y = 230f

        /** Сумма чека-основания: она же по умолчанию стоит суммой возврата. */
        const val TOTAL = 1_137_250L

        val ITEMS = listOf(
            SoldItem(
                name = "Баранина на косточке, охлаждённая",
                price = BigDecimal("3450.00"),
                quantityThousandths = 1_450,
                sum = BigDecimal("5002.50"),
                vatGroup = "VAT_16",
                measureUnitCode = "166"
            ),
            SoldItem(
                name = "Коньяк «Казахстан» 0,5 л",
                price = BigDecimal("4990.00"),
                quantityThousandths = 1_000,
                sum = BigDecimal("6370.00"),
                vatGroup = "VAT_16",
                measureUnitCode = "796"
            )
        )
    }
}
