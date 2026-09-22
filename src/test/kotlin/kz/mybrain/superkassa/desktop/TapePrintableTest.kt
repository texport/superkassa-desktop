package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.TapePrintable
import java.awt.image.BufferedImage
import java.awt.print.PageFormat
import java.awt.print.Paper
import java.awt.print.Printable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Лента на страницах принтера.
 *
 * Узел рисует ленту картинкой в несколько тысяч точек высотой, а бумага
 * у кассы — 58 или 80 мм. Растянутый на страницу Z-отчёт занимал десяток
 * листов, а чек становился плакатом; сжатый в один лист — нечитаем.
 * Поэтому ширина берётся из настроек кассы, а высота режется по страницам
 * в том же масштабе.
 */
class TapePrintableTest {

    /** Страница A4 с полями в полдюйма: столько же, сколько у принтера кассы. */
    private fun page(): PageFormat = PageFormat().apply {
        paper = Paper().apply {
            setSize(A4_WIDTH, A4_HEIGHT)
            setImageableArea(MARGIN, MARGIN, A4_WIDTH - MARGIN * 2, A4_HEIGHT - MARGIN * 2)
        }
    }

    private fun tape(height: Int) = BufferedImage(TAPE_DOTS, height, BufferedImage.TYPE_INT_ARGB)

    /** Сколько страниц занимает лента: печатаем, пока принтер берёт страницу. */
    private fun pages(image: BufferedImage, widthMm: Int, limit: Int = PAGE_LIMIT): Int {
        val printable = TapePrintable(image, widthMm)
        val page = page()
        val canvas = BufferedImage(A4_WIDTH.toInt(), A4_HEIGHT.toInt(), BufferedImage.TYPE_INT_ARGB)
        var count = 0
        while (count < limit) {
            val graphics = canvas.createGraphics()
            val answer = printable.print(graphics, page, count)
            graphics.dispose()
            if (answer == Printable.NO_SUCH_PAGE) return count
            count += 1
        }
        return count
    }

    /**
     * Чек на ленту 58 мм занимает один лист.
     *
     * Лента печатается своей шириной: 58 мм — это 164 точки страницы,
     * и картинка в 576 точек ужимается втрое с лишним. Высота чека при
     * этом ужимается так же, и лист остаётся один.
     */
    @Test
    fun `чек на узкой ленте ложится на один лист`() {
        assertEquals(1, pages(tape(SHORT_TAPE), 58))
    }

    /**
     * Z-отчёт длиной в несколько лент режется по страницам, а не жмётся
     * в одну: сжатый отчёт нечитаем, и кассир не сверит по нему смену.
     */
    @Test
    fun `длинный отчёт режется по страницам`() {
        val many = pages(tape(LONG_TAPE), 58)
        assertTrue(many > 1, "длинный отчёт уместился в $many страницу")
        assertTrue(many < PAGE_LIMIT, "печать не кончилась: $many страниц")
    }

    /**
     * «Полная страница» печатает по ширине печатного поля, и та же лента
     * занимает меньше листов, чем узкая: строки крупнее, но их столько же
     * на странице по высоте.
     */
    @Test
    fun `полная страница шире узкой ленты`() {
        val narrow = pages(tape(LONG_TAPE), 58)
        val full = pages(tape(LONG_TAPE), 0)
        assertTrue(full > narrow, "полная страница заняла $full против $narrow у ленты 58 мм")
    }

    /** Лента не растягивается сверх своей ширины: 58 мм уже, чем 80 мм. */
    @Test
    fun `узкая лента занимает больше страниц, чем широкая`() {
        assertTrue(pages(tape(LONG_TAPE), 58) <= pages(tape(LONG_TAPE), 80))
    }

    private companion object {
        const val A4_WIDTH = 595.0
        const val A4_HEIGHT = 842.0
        const val MARGIN = 36.0

        /** Ширина ленты, как её рисует узел: 576 точек у чековой печати. */
        const val TAPE_DOTS = 576

        const val SHORT_TAPE = 1200
        const val LONG_TAPE = 12000

        /** Предел страниц у проверки: без него ошибка в счёте страниц зациклит её. */
        const val PAGE_LIMIT = 200
    }
}
