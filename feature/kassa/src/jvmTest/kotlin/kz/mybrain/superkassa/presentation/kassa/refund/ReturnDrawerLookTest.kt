package kz.mybrain.superkassa.presentation.kassa.refund

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.ReturnsScene
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
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
@OptIn(ExperimentalCoroutinesApi::class)
class ReturnDrawerLookTest {

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    /** Панель возврата с выбранным чеком-основанием при заданном остатке ящика. */
    private fun panel(folder: String, drawerTiyn: Long): ByteArray {
        val model = ReturnsScene.model(listOf(ReturnsScene.sale(41, TOTAL)), ITEMS, drawer = drawerTiyn)
        return RenderProbe(
            width = KassaScene.WIDE,
            height = KassaScene.TALL,
            content = { ReturnsScreen(model) }
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
            ReturnsScene.item("Баранина на косточке, охлаждённая", "3450.00", 1_450, "5002.50", unit = "116"),
            ReturnsScene.item("Коньяк «Казахстан» 0,5 л", "4990.00", 1_000, "6370.00")
        )
    }
}
