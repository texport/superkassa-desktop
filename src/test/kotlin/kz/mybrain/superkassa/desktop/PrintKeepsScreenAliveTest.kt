package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.ServerClient
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Печать из списка не занимает поток экрана.
 *
 * Узел рисует ленту картинкой в тысячи точек высотой, и приложение
 * срезает у неё поля страницы прежде, чем отдать принтеру: проход
 * по всем точкам и заливка углов у длинного Z-отчёта занимают около
 * секунды. Просмотр уносил эту работу в поток ввода-вывода, а печать
 * прямо из списка — нет, и на всё это время касса переставала
 * отзываться: кассир жал «Печать» и получал застывший экран.
 *
 * Проверка смотрит не на код, а на поток экрана: пока идёт печать,
 * он обязан успевать делать свои дела.
 */
class PrintKeepsScreenAliveTest {

    /** Лента размером с Z-отчёт: поля по бокам, белая бумага посередине. */
    private fun tape(): ByteArray {
        val image = BufferedImage(TAPE_WIDTH, TAPE_HEIGHT, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().run {
            color = Color(PAGE_BACKGROUND)
            fillRect(0, 0, TAPE_WIDTH, TAPE_HEIGHT)
            color = Color.WHITE
            fillRect(MARGIN, 0, TAPE_WIDTH - 2 * MARGIN, TAPE_HEIGHT)
            color = Color.BLACK
            for (line in 0 until TAPE_HEIGHT step LINE_STEP) {
                fillRect(MARGIN + INDENT, line, TAPE_WIDTH - 2 * (MARGIN + INDENT), LINE_HEIGHT)
            }
            dispose()
        }
        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "PNG", out)
            out.toByteArray()
        }
    }

    private fun session(png: ByteArray): Session {
        val http = HttpClient(
            MockEngine { respond(png, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "image/png")) }
        )
        val directory = Files.createTempDirectory("print-alive").toFile()
        directory.deleteOnExit()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm"))).apply {
            select(Kkm(kkmId = "kkm-1"), remember = false)
            adoptPin("1234")
        }
    }

    /**
     * Самый долгий промежуток, в который поток экрана ничего не делал.
     *
     * Поток отмечается сам: очередная отметка ставит следующую. Пока
     * он свободен, отметки идут подряд; занятый работой, он молчит,
     * и молчание видно промежутком между соседними отметками.
     */
    private fun longestPause(work: () -> Unit): Long {
        val marks = mutableListOf<Long>()
        var alive = true
        fun mark() {
            if (!alive) return
            marks += System.currentTimeMillis()
            SwingUtilities.invokeLater(::mark)
        }
        SwingUtilities.invokeLater(::mark)
        work()
        Thread.sleep(WATCH_MS)
        alive = false
        val seen = marks.toList()
        return seen.zipWithNext { earlier, later -> later - earlier }.maxOrNull() ?: 0L
    }

    @Test
    fun `печать длинной ленты не останавливает поток экрана`() {
        val session = session(tape())

        val pause = longestPause { session.printDesk.printDocument("d-1") }

        assertTrue(pause < PAUSE_LIMIT_MS, "поток экрана стоял $pause мс: касса не отзывалась")
    }

    private companion object {
        /** Ширина холста узла: лента 80 мм с полями страницы, втрое плотнее точек CSS. */
        const val TAPE_WIDTH = 1140

        /** Высота с Z-отчёт смены. */
        const val TAPE_HEIGHT = 12_000

        /** Поле страницы по бокам ленты, как его рисует узел. */
        const val MARGIN = 116

        const val INDENT = 24
        const val LINE_STEP = 24
        const val LINE_HEIGHT = 8

        /** Фон страницы узла: почти белый, но не белый. */
        const val PAGE_BACKGROUND = 0xFAF8F5

        /** Сколько смотреть за потоком экрана после нажатия. */
        const val WATCH_MS = 3000L

        /** Дольше этого заминка уже видна кассиру. */
        const val PAUSE_LIMIT_MS = 250L
    }
}
