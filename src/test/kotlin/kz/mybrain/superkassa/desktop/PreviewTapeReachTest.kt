package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import kz.mybrain.superkassa.desktop.ui.components.ReceiptPreview
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Длинная печатная форма обязана прокручиваться до последней строки.
 *
 * Z-отчёт смены длиннее окна в несколько раз, и его последние блоки —
 * налоги, типы расчётов, кассовые операции. Недостижимый низ формы
 * кассир принимает за отсутствие этих блоков в документе.
 *
 * Проверяется без окна: форма рисуется в снимок сцены, колесо мыши
 * приходит теми же событиями, что от машины кассира. Признак низа —
 * синяя полоса в последних точках формы.
 *
 * Проверка сторожит только показ: длину самой формы задаёт узел, и если
 * он отдал обрезанный образ, показывать целое неоткуда.
 */
@OptIn(ExperimentalComposeUiApi::class)
class PreviewTapeReachTest {

    /** Форма длиной в несколько окон: строки по всей длине и синий низ. */
    private fun longForm(height: Int): ByteArray {
        val picture = BufferedImage(FORM_WIDTH, height, BufferedImage.TYPE_INT_RGB)
        val paint = picture.createGraphics()
        paint.color = Color.WHITE
        paint.fillRect(0, 0, picture.width, height)
        paint.color = Color.BLACK
        for (row in 20 until height - FOOT_HEIGHT step 40) paint.fillRect(40, row, 400, 6)
        paint.color = Color.BLUE
        paint.fillRect(0, height - FOOT_HEIGHT, picture.width, FOOT_HEIGHT)
        paint.dispose()
        val bytes = ByteArrayOutputStream()
        ImageIO.write(picture, "png", bytes)
        return bytes.toByteArray()
    }

    /**
     * Сколько на снимке синих точек.
     *
     * Сравниваются каналы между собой, а не с точным цветом: форма лежит
     * под полупрозрачной подложкой окна, и синий на снимке светлее того,
     * что нарисовано.
     */
    private fun footprint(shot: Image): Int {
        val bitmap = Bitmap.makeFromImage(shot)
        var seen = 0
        for (x in 0 until shot.width step SAMPLE_STEP) {
            for (y in 0 until shot.height step SAMPLE_STEP) {
                val colour = bitmap.getColor(x, y)
                val red = (colour shr RED_SHIFT) and CHANNEL
                val green = (colour shr GREEN_SHIFT) and CHANNEL
                val blue = colour and CHANNEL
                if (blue - red > BLUE_MARGIN && blue - green > BLUE_MARGIN) seen++
            }
        }
        return seen
    }

    @Test
    fun `низ длинной формы достигается прокруткой`() {
        val form = longForm(FORM_HEIGHT)
        val scene = ImageComposeScene(width = WINDOW_WIDTH, height = WINDOW_HEIGHT, density = Density(1f)) {
            CompositionLocalProvider(LocalStrings provides stringsOf(Language.Ru)) {
                ReceiptPreview(image = form, onDismiss = {})
            }
        }
        assertEquals(
            0,
            footprint(scene.render()),
            "низ формы виден сразу — форма короче окна, проверка ничего не стоит"
        )
        val reached = scrolledToFoot(scene)
        scene.close()
        assertTrue(reached, "низ формы недостижим прокруткой: колесо упирается раньше последней строки")
    }

    /** Крутит колесо, пока не покажется низ формы или пока круги не кончатся. */
    private fun scrolledToFoot(scene: ImageComposeScene): Boolean {
        var rounds = 0
        while (rounds < WHEEL_ROUNDS) {
            repeat(WHEEL_TICKS) {
                scene.sendPointerEvent(
                    eventType = PointerEventType.Scroll,
                    position = Offset(WINDOW_WIDTH / 2f, WINDOW_HEIGHT / 2f),
                    scrollDelta = Offset(0f, WHEEL_DELTA)
                )
                scene.render()
            }
            if (footprint(scene.render()) > 0) return true
            rounds++
        }
        return false
    }

    private companion object {
        /** Ширина печатной формы в точках — как её рисует узел. */
        const val FORM_WIDTH = 576

        /** Длина формы: столько занимает Z-отчёт смены с разбором налогов. */
        const val FORM_HEIGHT = 6000

        /** Высота синего низа формы. */
        const val FOOT_HEIGHT = 120

        const val WINDOW_WIDTH = 1200
        const val WINDOW_HEIGHT = 800

        /** Сколько кругов колеса и по сколько щелчков в круге. */
        const val WHEEL_ROUNDS = 40
        const val WHEEL_TICKS = 50
        const val WHEEL_DELTA = 30f

        /** Каждая какая точка снимка попадает в выборку. */
        const val SAMPLE_STEP = 7

        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val CHANNEL = 0xFF

        /** Насколько синий канал обгоняет остальные, чтобы считаться синим. */
        const val BLUE_MARGIN = 40
    }
}
