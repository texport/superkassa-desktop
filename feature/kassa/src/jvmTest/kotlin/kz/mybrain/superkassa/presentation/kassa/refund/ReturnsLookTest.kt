package kz.mybrain.superkassa.presentation.kassa.refund

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.ReturnsScene
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Возврат: обычный ход и отказные случаи, панель с выбранным чеком.
 *
 * Снимки — `/tmp/kassa-return-*.png`. Выбор чека делается нажатием
 * по строке списка, как у кассира.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReturnsLookTest {

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun sold(no: Long, tiyn: Long) = ReturnsScene.sale(no, tiyn)

    /** Возврат с открытой сменой и прочитанным днём. */
    private val returns =
        ReturnsUiState(kkm = CoreScene.kkm(), signedIn = true, shiftOpen = true, loading = false, dayRead = true)

    @Test
    fun `возврат собирается во всех состояниях и они различимы`() {
        val frames = mapOf(
            "shift-closed" to KassaScene.shot("return-shift-closed") {
                ReturnsContent(returns.copy(shiftOpen = false, documents = listOf(sold(41, 1_137_250))))
            },
            "kkm-blocked" to KassaScene.shot("return-kkm-blocked") {
                // Смена у снятой с учёта кассы остаётся открытой, и без
                // своего состояния экран предлагал кнопку возврата, на
                // которую касса отвечает KKM_BLOCKED.
                val blocked = CoreScene.kkm(state = "BLOCKED")
                ReturnsContent(returns.copy(kkm = blocked, documents = listOf(sold(41, 1_137_250))))
            },
            "no-basis" to KassaScene.shot("return-no-basis") { ReturnsContent(returns) },
            "basis-list" to KassaScene.shot("return-basis-list") {
                val day = listOf(sold(41, 1_137_250), sold(42, 69_000), sold(43, 499_000))
                ReturnsContent(returns.copy(documents = day))
            },
            "kassa-silent" to KassaScene.shot("return-kassa-silent") { ReturnsContent(returns.copy(dayRead = false)) }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        // Молчащая касса входит в набор наравне с остальными: кассир при
        // покупателе с чеком в руках читал «подходящих чеков-оснований нет»
        // как отказ в возврате, а касса о чеках покупателя ничего не сказала.
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "состояния возврата неотличимы друг от друга"
        )
    }

    /**
     * Панель возврата с выбранным чеком-основанием.
     *
     * Выбор делается нажатием по строке списка: панель до выбора пуста,
     * и всё, ради чего экран открыт, появляется только после него.
     */
    @Test
    fun `панель возврата с выбранным чеком показывает сумму и позиции`() {
        val model = ReturnsScene.model(listOf(sold(41, 1_137_250)), ITEMS)
        RenderProbe(
            width = KassaScene.WIDE,
            height = KassaScene.TALL,
            content = { ReturnsScreen(model) }
        ).use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            probe.click(androidx.compose.ui.geometry.Offset(BASIS_X, BASIS_Y))
            repeat(SETTLE) { probe.frame() }
            val after = probe.frame()
            java.io.File("/tmp/kassa-return-basis-chosen.png").writeBytes(after)

            assertTrue(!after.contentEquals(before), "чек-основание не выбирается нажатием")
        }
    }

    /**
     * Чек-основание из десятка позиций не выдавливает кнопку за край.
     *
     * Состав чека узел отдаёт целиком, и у обычной продуктовой корзины
     * строк бывает полтора десятка. Прежде список рос вниз без прокрутки
     * и уносил за нижний край панели поле суммы, виды оплаты и само
     * «Вернуть покупателю» — вернуть деньги было нечем.
     */
    @Test
    fun `длинный состав чека не уносит кнопку возврата за край панели`() {
        val short = refundPanel("ret-short", ITEMS)
        val long = refundPanel("ret-long", MANY)

        assertTrue(
            bottomStrip(short).contentEquals(bottomStrip(long)),
            "кнопка возврата смещается от числа позиций в чеке-основании"
        )
    }

    /** Панель возврата с выбранным чеком: снимок после нажатия по списку. */
    private fun refundPanel(folder: String, items: List<ReceiptItemView>): ByteArray {
        val total = items.sumOf { Tenge.of(it.sum) }
        val model = ReturnsScene.model(listOf(sold(41, total)), items)
        return RenderProbe(
            width = KassaScene.WIDE,
            height = KassaScene.TALL,
            content = { ReturnsScreen(model) }
        ).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(androidx.compose.ui.geometry.Offset(BASIS_X, BASIS_Y))
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            java.io.File("/tmp/kassa-$folder.png").writeBytes(frame)
            frame
        }
    }

    /** Нижняя полоса кадра: в ней стоит единственное действие панели. */
    private fun bottomStrip(png: ByteArray): IntArray {
        val image = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png))
        val from = image.height - BUTTON_STRIP
        return image.getRGB(0, from, image.width, BUTTON_STRIP, null, 0, image.width)
    }

    private companion object {
        const val SETTLE = 40

        /** Высота полосы у нижнего края, в которой стоит кнопка возврата. */
        const val BUTTON_STRIP = 60

        /** Первая строка списка чеков-оснований: по ней и щёлкаем. */
        const val BASIS_X = 300f
        const val BASIS_Y = 230f

        val ITEMS = listOf(
            ReturnsScene.item("Баранина на косточке, охлаждённая", "3450.00", 1_450, "5002.50", unit = "116"),
            ReturnsScene.item("Коньяк «Казахстан» 0,5 л", "4990.00", 1_000, "4990.00")
        )

        /** Обычная продуктовая корзина: строк больше, чем влезает в панель. */
        val MANY = (1..14).map { at ->
            ReturnsScene.item("Товар с довольно длинным наименованием номер $at", "990.00", 1_000, "990.00")
        }
    }
}
