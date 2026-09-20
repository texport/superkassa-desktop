package kz.mybrain.superkassa.desktop.app

import java.awt.image.BufferedImage
import java.awt.print.PrinterJob
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.print.PrintService

/**
 * Печать на настоящий принтер и сохранение в файл.
 *
 * Печатается тот же образ, который рисует узел: свой рисунок дал бы два
 * разных чека по одному документу. Лента длиннее страницы разрезается по
 * страницам, а не сжимается — сжатый Z-отчёт нечитаем.
 */
object Printing {

    /** Принтеры, которые видит эта машина. */
    fun printers(): List<String> = PrinterJob.lookupPrintServices().map { it.name }

    /** Принтер по умолчанию системы; `null`, если его нет. */
    fun systemPrinter(): String? = PrinterJob.getPrinterJob().printService?.name

    /**
     * Печатает образ документа.
     *
     * @param image печатная форма документа в PNG — как её нарисовал узел.
     * @param printer имя принтера; `null` — принтер по умолчанию системы.
     * @return `true`, если задание ушло на принтер.
     */
    fun print(image: ByteArray, printer: String?, tapeWidthMm: Int, copies: Int = 1): Boolean {
        val picture = ImageIO.read(ByteArrayInputStream(image)) ?: return false
        val job = PrinterJob.getPrinterJob()
        printer?.let { name -> serviceNamed(name)?.let { job.printService = it } }
        job.setPrintable(TapePrintable(picture, tapeWidthMm))
        job.copies = copies.coerceAtLeast(1)
        job.print()
        return true
    }

    /**
     * Обрезает поля страницы вокруг ленты.
     *
     * Узел рисует ленту по центру страницы, и по бокам остаётся её фон.
     * На экране кассир принимал его за часть документа, а на печати он
     * съедал ширину бумаги.
     *
     * Сравнение с допуском, а не точное: под лентой лежит мягкая тень,
     * и по точному цвету полем считались бы только два десятка пикселей
     * у самого края.
     *
     * @param image печатная форма в PNG.
     * @return та же форма без полей страницы.
     */
    fun trim(image: ByteArray): ByteArray {
        val picture = ImageIO.read(ByteArrayInputStream(image)) ?: return image
        val background = picture.getRGB(0, 0)
        val left = (0 until picture.width).firstOrNull { !isColumnMargin(picture, it, background) }
        val right = (picture.width - 1 downTo 0).firstOrNull { !isColumnMargin(picture, it, background) }
        val top = (0 until picture.height).firstOrNull { !isRowMargin(picture, it, background) }
        val bottom = (picture.height - 1 downTo 0).firstOrNull { !isRowMargin(picture, it, background) }
        val horizontal = if (left != null && right != null && left < right) left..right else return image
        val vertical = if (top != null && bottom != null && top < bottom) top..bottom else return image
        val cropped = picture.getSubimage(
            horizontal.first,
            vertical.first,
            horizontal.last - horizontal.first + 1,
            vertical.last - vertical.first + 1
        )
        val bytes = ByteArrayOutputStream()
        ImageIO.write(clearCorners(cropped, background), "png", bytes)
        return bytes.toByteArray()
    }

    /**
     * Делает фон страницы прозрачным.
     *
     * У ленты скруглённый верх, и в углах обрезанной картинки остаётся
     * фон страницы: на тёмной теме он светлыми уголками, на печати —
     * серой заливкой. Заливка идёт от краёв, поэтому белое поле внутри
     * чека остаётся белым.
     */
    private fun clearCorners(picture: BufferedImage, background: Int): BufferedImage {
        val clear = BufferedImage(picture.width, picture.height, BufferedImage.TYPE_INT_ARGB)
        clear.createGraphics().apply {
            drawImage(picture, 0, 0, null)
            dispose()
        }
        val queue = ArrayDeque<Int>()
        val seen = BooleanArray(clear.width * clear.height)
        for (column in 0 until clear.width) {
            queue.add(column)
            queue.add(column + (clear.height - 1) * clear.width)
        }
        for (row in 0 until clear.height) {
            queue.add(row * clear.width)
            queue.add(clear.width - 1 + row * clear.width)
        }
        while (queue.isNotEmpty()) {
            val at = queue.removeFirst()
            if (isPageBackground(clear, seen, at, background)) {
                seen[at] = true
                clear.setRGB(at % clear.width, at / clear.width, TRANSPARENT)
                addNeighbours(queue, clear, at)
            }
        }
        return clear
    }

    /** Пиксель ещё не пройден и по цвету — фон страницы. */
    private fun isPageBackground(picture: BufferedImage, seen: BooleanArray, at: Int, background: Int): Boolean {
        if (at < 0 || at >= seen.size || seen[at]) return false
        return isBackground(picture.getRGB(at % picture.width, at / picture.width), background)
    }

    /** Соседи пикселя слева, справа, сверху и снизу — без выхода за край. */
    private fun addNeighbours(queue: ArrayDeque<Int>, picture: BufferedImage, at: Int) {
        val column = at % picture.width
        val row = at / picture.width
        if (column > 0) queue.add(at - 1)
        if (column < picture.width - 1) queue.add(at + 1)
        if (row > 0) queue.add(at - picture.width)
        if (row < picture.height - 1) queue.add(at + picture.width)
    }

    private fun isColumnMargin(picture: BufferedImage, column: Int, background: Int): Boolean =
        (0 until picture.height).all { row -> isBackground(picture.getRGB(column, row), background) }

    private fun isRowMargin(picture: BufferedImage, row: Int, background: Int): Boolean =
        (0 until picture.width).all { column -> isBackground(picture.getRGB(column, row), background) }

    /** Цвет считается фоном, пока отличается не сильнее допуска. */
    private fun isBackground(colour: Int, background: Int): Boolean =
        CHANNELS.all { shift ->
            val one = (colour shr shift) and CHANNEL_MASK
            val two = (background shr shift) and CHANNEL_MASK
            kotlin.math.abs(one - two) <= MARGIN_TOLERANCE
        }

    /** Сохраняет полученные от узла байты в файл. */
    fun save(bytes: ByteArray, target: File) {
        target.writeBytes(bytes)
    }

    private fun serviceNamed(name: String): PrintService? =
        PrinterJob.lookupPrintServices().firstOrNull { it.name == name }
}
