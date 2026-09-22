package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.analytics.SalesNetworkPlates
import kz.mybrain.superkassa.desktop.ui.analytics.SalesView
import kz.mybrain.superkassa.desktop.ui.theme.LightScheme
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Строка состояния сети на боевых числах кабинета показа.
 *
 * В сети показа из 3294 касс 3288 — черновики, которым КГД учёта ещё
 * не дал: чеков за срок от них не приходило и прийти не могло. Строка
 * называла их молчащими и красила отказом, и перед гостем стояло красное
 * «3 289» о хозяйстве, в котором чинить нечего.
 *
 * Проверяется самой картинкой: роль отказа в строке — это цвет, а цвет
 * счётом не проверишь.
 */
class AnalyticsSalesRenderShowTest {

    @Test
    fun `кассы без чеков за срок не красятся отказом`() {
        val view = SalesLook.show().let {
            // Остальные числа строки обнулены: красное в кадре должно
            // остаться только от касс без чеков, иначе проверка молчит
            // о том, ради чего заведена.
            it.copy(summary = it.summary.copy(offlineCount = 0, queuedCount = 0, unknownCount = 0))
        }
        assertTrue(view.summary.cashRegisterCount > view.registers.count { it.receiptCount > 0 })
        assertFalse(refusalInside(plates(view)), "касса без чеков за срок покрашена отказом")
    }

    /** Отказ в кадре всё-таки видно: иначе проверка выше прошла бы и на пустом снимке. */
    @Test
    fun `отбракованные документы отказом красятся`() {
        val view = SalesLook.show().let { it.copy(summary = it.summary.copy(offlineCount = 3)) }
        assertTrue(refusalInside(plates(view)), "автономные документы остались без цвета отказа")
    }

    private fun plates(view: SalesView) = RenderProbe(WIDTH, HEIGHT) {
        SalesNetworkPlates(view, Look.texts.sales, Modifier.fillMaxSize())
    }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        probe.frame()
    }

    /** Есть ли в кадре хоть одна точка роли отказа. */
    private fun refusalInside(png: ByteArray): Boolean {
        val image = ImageIO.read(ByteArrayInputStream(png))
        val refused = LightScheme.error.value.toULong().shr(BITS).toInt()
        val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        return pixels.any { it and RGB == refused and RGB }
    }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 120
        const val SETTLE = 12

        /** Цвет Compose лежит в старших разрядах длинного числа. */
        const val BITS = 32

        /** Сравниваются только цветовые разряды: прозрачность в кадре своя. */
        const val RGB = 0x00FFFFFF
    }
}
