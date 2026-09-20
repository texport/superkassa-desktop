package kz.mybrain.superkassa.desktop.eds

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Подпись ЭЦП через NCALayer.
 *
 * Ключ и пароль к нему остаются у владельца: приложение отправляет
 * в NCALayer то, что нужно подписать, а окно выбора сертификата и ввода
 * пароля показывает сам NCALayer. Ни ключ, ни пароль сюда не приходят
 * и нигде не хранятся.
 *
 * NCALayer слушает петлю по защищённому соединению с самоподписанным
 * сертификатом на `127.0.0.1`. Проверять его нечем и незачем: собеседник
 * — процесс на этой же машине, а не узел в сети. Доверие ограничено
 * этим соединением и не распространяется на обмен с кабинетом и узлом.
 */
class NcaLayer(private val address: String = DEFAULT_ADDRESS) {

    /**
     * Подписывает содержимое и возвращает CMS в base64.
     *
     * @param base64Content то, что подписывается, в base64 — как его выдал кабинет.
     * @return CMS-подпись в base64.
     * @throws EdsRefusal если NCALayer недоступен или владелец отказался подписывать.
     */
    suspend fun signCms(base64Content: String): String {
        val modern = runCatching { ncaSignatureOf(ask(ncaSignRequest(base64Content))) }
        modern.getOrNull()?.let { return it }
        val refusal = modern.exceptionOrNull() as? EdsRefusal ?: throw modern.exceptionOrNull()!!
        // Владелец закрыл окно — это его решение, повторять нечего.
        // Всё остальное может означать, что NCALayer этой версии
        // не знает модуль `basics`: у него есть прежний, и он работает
        // во всех выпусках, что стоят у владельцев.
        if (refusal.cancelled || refusal.problem != EdsProblem.Declined) throw refusal
        return ncaLegacySignatureOf(ask(ncaLegacyRequest(base64Content)))
    }

    /** Отвечает ли NCALayer на этой машине. */
    suspend fun available(): Boolean = runCatching {
        withTimeout(PROBE_TIMEOUT_MS) { client().use { it.webSocket(address) { } } }
    }.isSuccess

    /**
     * Запрос и ответ на него.
     *
     * NCALayer здоровается первым: сразу после подключения он присылает
     * кадр со своей версией. Принимать его за ответ нельзя — подпись
     * приходит следующим кадром, и приложение выбрасывало её, считая,
     * что владелец отказался подписывать, а затем открывало окно подписи
     * во второй раз. Поэтому кадры читаются, пока не придёт ответ по делу.
     *
     * Ожидание длинное: NCALayer держит соединение, пока владелец выбирает
     * сертификат и вводит пароль, и пять секунд оборвали бы подпись
     * на середине.
     */
    private suspend fun ask(request: JsonObject): JsonObject {
        val answer = try {
            client().use { http ->
                var received: JsonObject? = null
                withTimeout(SIGN_TIMEOUT_MS) {
                    http.webSocket(address) {
                        send(Frame.Text(request.toString()))
                        while (received == null) {
                            val text = (incoming.receive() as? Frame.Text)?.readText() ?: continue
                            received = parsed(text)?.takeUnless { it.isGreeting() }
                        }
                    }
                }
                received
            }
        } catch (failure: ClosedReceiveChannelException) {
            // Окно подписи закрыли, не ответив: NCALayer рвёт соединение
            // молча. Прежде это доходило до экрана именем класса — владелец
            // видел «Кабинет не отвечает» там, где сам и закрыл окно.
            throw EdsRefusal(EdsProblem.Declined, WINDOW_CLOSED, failure)
        } catch (failure: IOException) {
            throw EdsRefusal(EdsProblem.Unreachable, failure.message.orEmpty(), failure)
        } catch (failure: TimeoutCancellationException) {
            throw EdsRefusal(EdsProblem.Unreachable, failure.message.orEmpty(), failure)
        }
        return answer ?: throw EdsRefusal(EdsProblem.Unreachable, "")
    }

    /** Разбирает кадр; неразборный кадр ответом не считается. */
    private fun parsed(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()

    private fun client(): HttpClient = HttpClient(CIO) {
        install(WebSockets)
        engine {
            https {
                trustManager = LocalhostTrust
                random = SecureRandom()
            }
        }
    }

    companion object {
        /** Где NCALayer слушает на этой машине. */
        const val DEFAULT_ADDRESS: String = "wss://127.0.0.1:13579"

        /**
         * Владелец закрыл окно подписи.
         *
         * Здесь стоит код, а не готовая строка: слова подбирает показ
         * на языке владельца, а по-русски посреди кода их читал бы
         * и казах, и англичанин.
         */
        const val WINDOW_CLOSED: String = "WINDOW_CLOSED"

        private const val SIGN_TIMEOUT_MS = 180_000L
        private const val PROBE_TIMEOUT_MS = 2_000L

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

/**
 * Доверие соединению с петлёй.
 *
 * Сертификат NCALayer самоподписан и меняется при переустановке;
 * проверять его цепочку не у кого. Собеседник опознан адресом
 * `127.0.0.1`, дальше этого соединения доверие не идёт.
 */
private object LocalhostTrust : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
