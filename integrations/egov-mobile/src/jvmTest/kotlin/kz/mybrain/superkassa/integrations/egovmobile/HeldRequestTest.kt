package kz.mybrain.superkassa.integrations.egovmobile

import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Удержанный посредником запрос не обрывается по сроку тишины сокета.
 *
 * SIGEX держит передачу данных, пока eGov mobile их не заберёт, — всё время,
 * пока владелец сканирует QR, по соединению не идёт ни байта. Проверка — на
 * настоящем сокете и движке Android (OkHttp): подставной движок Ktor сроков
 * сокета не знает, а движок JDK срок тишины сокета не применяет вовсе.
 */
class HeldRequestTest {
    private val server = ServerSocket(0)

    @AfterTest
    fun close() = server.close()

    @Test
    fun `данные уходят, даже когда посредник держит их дольше срока ответа`() = runBlocking {
        serve()
        val relay = "http://127.0.0.1:${server.localPort}"
        val settings = EgovSettings(relay = relay, answerWait = QUICK, signWindow = 10.seconds)
        val egov = EgovMobile(settings, engine = OkHttp.create())
        val procedure = egov.open("AA==", EgovDocument("Superkassa", "Подпись", "Қолтаңба", "Signature"))
        assertEquals("MIIC", egov.await(procedure))
    }

    /** Посредник: регистрация сразу, данные — после паузы дольше срока ответа, подпись сразу. */
    private fun serve() = thread(isDaemon = true) {
        while (!server.isClosed) {
            val socket = runCatching { server.accept() }.getOrNull() ?: break
            socket.use { answer(it) }
        }
    }

    private fun answer(socket: Socket) {
        val head = socket.getInputStream().bufferedReader().let { reader ->
            generateSequence { reader.readLine() }.takeWhile { it.isNotEmpty() }.toList()
        }
        val body = when {
            head.first().startsWith("POST /api/egovQr ") -> registered()
            head.first().startsWith("POST /data") -> """{"signURL":"${base()}/sign"}""".also { Thread.sleep(HELD) }
            else -> SIGNED
        }
        val bytes = body.encodeToByteArray()
        socket.getOutputStream().apply {
            val status = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n" +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
            write(status.encodeToByteArray())
            write(bytes)
            flush()
        }
    }

    private fun base() = "http://127.0.0.1:${server.localPort}"

    private fun registered() = """{"qrCode":"iVBORw==","eGovMobileLaunchLink":"https://l",""" +
        """"dataURL":"${base()}/data","signURL":"${base()}/sign"}"""

    private companion object {
        /**
         * Срок ответа — с запасом на холодный первый запрос: на машине
         * GitHub OkHttp поднимался дольше 0,4 с, и регистрация падала
         * по сроку раньше, чем доходило до удержанных данных.
         */
        val QUICK = 2.seconds

        /** Посредник держит данные заведомо дольше срока ответа. */
        const val HELD = 3000L
        const val SIGNED = """{"documentsToSign":[{"id":1,"nameRu":"","nameKz":"","nameEn":"",""" +
            """"document":{"file":{"data":"MIIC"}}}]}"""
    }
}
