package kz.mybrain.superkassa.desktop.app

import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.print.PageFormat
import java.awt.print.Printable

/**
 * Лента на страницах принтера.
 *
 * Ширина берётся из настроек кассы: 58 или 80 мм — столько же, сколько
 * у бумаги в принтере. Если лента шире печатного поля страницы — так
 * бывает на «полной странице» и на узком поле A4, — она ужимается
 * до поля, но не растягивается сверх своей ширины.
 *
 * Высота режется по страницам в том же масштабе: ужать отчёт в одну
 * страницу значит сделать его нечитаемым.
 */
internal class TapePrintable(
    private val picture: BufferedImage,
    private val tapeWidthMm: Int
) : Printable {

    override fun print(graphics: Graphics, page: PageFormat, index: Int): Int {
        // Лента печатается своей шириной — той, что задана в настройках
        // кассы, — а не растягивается на всю страницу. Растянутый на A4
        // Z-отчёт занимал десяток листов, а чек становился плакатом.
        val scale = tapeScale(page)
        val pageHeight = page.imageableHeight / scale
        val from = (index * pageHeight).toInt()
        if (from >= picture.height) return Printable.NO_SUCH_PAGE
        val height = minOf(pageHeight.toInt(), picture.height - from)
        val canvas = graphics as Graphics2D
        canvas.translate(page.imageableX, page.imageableY)
        canvas.scale(scale, scale)
        canvas.drawImage(
            picture.getSubimage(0, from, picture.width, height),
            0,
            0,
            null
        )
        return Printable.PAGE_EXISTS
    }

    /**
     * Во сколько раз образ уменьшается на бумаге.
     *
     * Ширина ленты в миллиметрах переводится в точки страницы (72 точки
     * на дюйм), а «полная страница» печатается по ширине печатного поля.
     */
    private fun tapeScale(page: PageFormat): Double {
        val requested = if (tapeWidthMm > 0) {
            tapeWidthMm * POINTS_IN_INCH / MM_IN_INCH
        } else {
            page.imageableWidth
        }
        return minOf(requested, page.imageableWidth) / picture.width
    }
}

/** Точек в дюйме у страницы принтера. */
private const val POINTS_IN_INCH = 72.0

/** Миллиметров в дюйме. */
private const val MM_IN_INCH = 25.4

/** Сдвиги красной, зелёной и синей доли цвета. */
internal val CHANNELS = intArrayOf(0, 8, 16)

/** Одна доля цвета — один байт. */
internal const val CHANNEL_MASK = 0xFF

/** Насколько тень под лентой ещё считается фоном страницы. */
internal const val MARGIN_TOLERANCE = 8

/** Полностью прозрачный пиксель. */
internal const val TRANSPARENT = 0
