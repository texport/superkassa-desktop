package kz.mybrain.superkassa.desktop

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.time.Duration

/**
 * Подставной NCALayer: вебсокет на свободном порту.
 *
 * Живой NCALayer в проверках не участвует: подписать он может только
 * ключом владельца и только руками. А проверять надо как раз то, чего
 * ключ не касается: молчание после принятого запроса, закрытое окно,
 * отказ и повтор прежним модулем.
 *
 * Слушает `ws://`, а не `wss://`: защищённое соединение с петлёй
 * проверяет доверие к самоподписанному сертификату, а здесь проверяется
 * разговор. Адрес приложение берёт из настроек, и подмена его — вся
 * оснастка.
 *
 * @param answers чем ответить на пришедший запрос.
 */
internal class NcaFake(private val answers: (String) -> NcaReply) : AutoCloseable {

    private val server = ServerSocket(0, BACKLOG, InetAddress.getByName("127.0.0.1"))

    /** Куда указать приложению. */
    val address: String = "ws://127.0.0.1:${server.localPort}"

    /** Запросы, которые дошли: по ним видно, кого спросили и сколько раз. */
    val asked: MutableList<String> = CopyOnWriteArrayList()

    init {
        thread(isDaemon = true, name = "nca-fake") {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: return@thread
                thread(isDaemon = true) { runCatching { serve(socket) } }
            }
        }
    }

    override fun close() = server.close()

    /** Один разговор: рукопожатие, приветствие, запрос и ответ на него. */
    private fun serve(socket: Socket) = socket.use {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val key = upgradeKey(input) ?: return@use
        output.write(accepted(key).toByteArray())
        output.flush()
        // NCALayer здоровается первым кадром со своей версией — приложение
        // обязано не принять его за ответ.
        output.writeFrame(GREETING)
        val request = input.readFrame() ?: return@use
        asked += request
        if (answer(answers(request), output)) awaitClose(input)
    }

    /**
     * Пока приложение не закрыло соединение, разговор не кончился:
     * на молчании оно закроет его по своему сроку или по отмене.
     */
    private fun awaitClose(input: InputStream) {
        runCatching { while (input.read() != -1) { /* ждём закрытия */ } }
    }

    /** Ответ на запрос; `false` — соединение закрывается, не ответив. */
    private fun answer(reply: NcaReply, output: OutputStream): Boolean = when (reply) {
        is NcaReply.Frames -> {
            reply.texts.forEach { output.writeFrame(it) }
            true
        }
        is NcaReply.Closed -> {
            Thread.sleep(reply.after.inWholeMilliseconds)
            false
        }
        NcaReply.Silence -> true
    }

    /** Ключ рукопожатия из заголовков запроса. */
    private fun upgradeKey(input: InputStream): String? {
        val head = StringBuilder()
        while (!head.endsWith("\r\n\r\n")) {
            val byte = input.read()
            if (byte == -1) return null
            head.append(byte.toChar())
        }
        return head.lines()
            .firstOrNull { it.startsWith(KEY_HEADER, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
    }

    /** Ответ рукопожатия: тот же ключ, подписанный по RFC 6455. */
    private fun accepted(key: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest((key + MAGIC).toByteArray())
        return "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\nConnection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: ${Base64.getEncoder().encodeToString(digest)}\r\n\r\n"
    }

    /** Кадр от приложения: он всегда с маской. */
    private fun InputStream.readFrame(): String? {
        val first = read()
        if (first == -1 || (first and OPCODE) != TEXT) return null
        val flagged = read()
        if (flagged == -1) return null
        val length = length(flagged and LENGTH)
        val mask = ByteArray(MASK_SIZE) { read().toByte() }
        val payload = ByteArray(length) { (read().toByte().toInt() xor mask[it % MASK_SIZE].toInt()).toByte() }
        return String(payload)
    }

    /** Длина кадра: короткая, двухбайтовая или восьмибайтовая. */
    private fun InputStream.length(short: Int): Int = when (short) {
        LENGTH_SHORT -> (0 until 2).fold(0) { sum, _ -> sum shl BITS or read() }
        LENGTH_LONG -> (0 until 8).fold(0) { sum, _ -> sum shl BITS or read() }
        else -> short
    }

    /** Кадр приложению: от сервера он без маски. */
    private fun OutputStream.writeFrame(text: String) {
        val payload = text.toByteArray()
        write(TEXT or FINAL)
        if (payload.size < LENGTH_SHORT) {
            write(payload.size)
        } else {
            write(LENGTH_SHORT)
            write(payload.size shr BITS and BYTE)
            write(payload.size and BYTE)
        }
        write(payload)
        flush()
    }

    private companion object {
        const val GREETING = """{"result":{"version":"1.4"}}"""
        const val MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        const val KEY_HEADER = "Sec-WebSocket-Key"
        const val BACKLOG = 16
        const val FINAL = 0x80
        const val OPCODE = 0x0f
        const val TEXT = 0x01
        const val LENGTH = 0x7f
        const val LENGTH_SHORT = 126
        const val LENGTH_LONG = 127
        const val MASK_SIZE = 4
        const val BITS = 8
        const val BYTE = 0xff
    }
}

/** Чем подставной NCALayer отвечает на запрос. */
internal sealed interface NcaReply {

    /** Запрос принят, ответа нет: так молчит занятый NCALayer. */
    data object Silence : NcaReply

    /**
     * Соединение закрывается без ответа.
     *
     * Сразу — так отвечает выпуск, который не знает модуль `basics`:
     * окна подписи владелец не видел. Спустя время — окно было, и закрыл
     * его владелец.
     */
    data class Closed(val after: Duration = Duration.ZERO) : NcaReply

    /** Ответ кадрами — подписью или отказом. */
    data class Frames(val texts: List<String>) : NcaReply
}
