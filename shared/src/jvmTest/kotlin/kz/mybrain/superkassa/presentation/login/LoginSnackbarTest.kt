package kz.mybrain.superkassa.presentation.login

import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Отказ входа не закрывает поле пина и кнопку «Войти».
 *
 * В окне 1000×700 на самой крупной ступени шрифта снекбар ложился
 * на нижнюю треть полосы пина — то есть ровно на то, что кассир должен
 * исправить, прочитав «Пользователь не найден или неверный PIN».
 *
 * Кадры остаются в `/tmp/loose-ends-login-snackbar-*.png`.
 */
class LoginSnackbarTest {

    private fun frame(name: String, refused: Boolean): BufferedImage {
        val app = CoreScene.app(LoginScene.core(listOf(KassaScene.kkm())))
        if (refused) app.notices.show(Message.Refusal(WRONG_PIN, "USER_NOT_FOUND"))
        val png = inlineMain {
            RenderProbe(
                width = WIDE,
                height = TALL,
                look = Look(textScale = TextScale.Larger)
            ) { LoginScene.Door(app) }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                probe.frame()
            }
        }
        File("/tmp/loose-ends-login-snackbar-$name.png").writeBytes(png)
        return ImageIO.read(ByteArrayInputStream(png))
    }

    /**
     * Где на кадре стоит нижняя полоса окна.
     *
     * Полоса — единственная поверхность у нижнего края: её тональная
     * подложка отличается от фона окна, и снизу вверх по середине кадра
     * она читается непрерывным куском. Границы берутся из кадра, а не
     * из чисел разметки: иначе проверка держалась бы на отступах, а не
     * на том, что видит кассир.
     */
    private fun barRows(image: BufferedImage): IntRange {
        val backdrop = image.getRGB(EDGE, image.height - EDGE)
        val middle = image.width / 2
        val bottom = (image.height - 1 downTo 0).first { image.getRGB(middle, it) != backdrop }
        val top = (bottom downTo 0).takeWhile { image.getRGB(middle, it) != backdrop }.last()
        return top..bottom
    }

    @Test
    fun `снекбар отказа входа не ложится на полосу пина`() {
        val quiet = frame("quiet", refused = false)
        val refused = frame("refused", refused = true)
        val bar = barRows(quiet)
        val snackbar = changedRows(quiet, refused)

        assertTrue(bar.last - bar.first > THIN, "полосы пина на кадре не нашлось: мерить нечего")
        assertFalse(snackbar.isEmpty(), "снекбар на кадре не появился: проверять перекрытие нечем")
        assertTrue(
            snackbar.last < bar.first,
            "снекбар лёг на полосу пина и закрыл ровно то, что кассир должен исправить: " +
                "строки снекбара $snackbar, полоса пина $bar"
        )
    }

    /**
     * Строки кадра, которые появление снекбара изменило.
     *
     * Снекбар — единственное отличие двух кадров, и его след виден прямо
     * в них: числа разметки для этого не нужны, а проверка по ним
     * держалась бы на отступах, а не на том, что видит кассир.
     */
    private fun changedRows(quiet: BufferedImage, refused: BufferedImage): IntRange {
        val changed = (0 until quiet.height).filter { row ->
            (0 until quiet.width).any { quiet.getRGB(it, row) != refused.getRGB(it, row) }
        }
        return if (changed.isEmpty()) IntRange.EMPTY else changed.first()..changed.last()
    }

    private companion object {
        /** Ноутбук кассира: самое тесное окно, за которым работают. */
        const val WIDE = 1000
        const val TALL = 700
        const val SETTLE = 60

        /** Отступ от края кадра, где заведомо виден фон окна. */
        const val EDGE = 2

        /** Ниже этого полоса пина была бы не полосой, а случайной строкой пикселей. */
        const val THIN = 40

        const val WRONG_PIN = "Пользователь не найден или неверный PIN"
    }
}
