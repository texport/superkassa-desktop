package kz.mybrain.superkassa.integrations.ncalayer.socket

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kz.mybrain.superkassa.integrations.ncalayer.NcaJournal
import kz.mybrain.superkassa.integrations.ncalayer.NcaSettings
import kz.mybrain.superkassa.integrations.ncalayer.protocol.isGreeting
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaAddressee
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Соединение с NCALayer: стук в дверь и запрос с ответом. */
internal class NcaSocket(private val settings: NcaSettings, private val journal: NcaJournal) {

    /**
     * Стук в дверь: соединение поднято и тут же оборвано.
     *
     * Здороваться незачем — нужен только ответ, отвечает ли NCALayer вообще;
     * а прощание зависит от собеседника и висло бы.
     */
    suspend fun knock(http: HttpClient) {
        http.webSocketSession(settings.address).cancel()
    }

    /**
     * Запрос и первый ответ по делу.
     *
     * Имя просителя NCALayer берёт из заголовка `Origin` рукопожатия: продукт
     * рассчитан на браузер. Без него окно подписи называло просителя
     * `UNDENTIFIED`. Соединение обрывается сразу, как ответ получен: NCALayer
     * закрывающего кадра не присылает, и вежливое закрытие ждало бы его до
     * срока, выбросив полученную подпись.
     */
    suspend fun answerTo(http: HttpClient, request: JsonObject, sent: SentMark): JsonObject? {
        val session = http.webSocketSession(settings.address) { header(HttpHeaders.Origin, settings.origin) }
        return try {
            session.send(Frame.Text(request.toString()))
            sent.mark = TimeSource.Monotonic.markNow()
            journal.record("NCALayer: request sent: ${ncaAddressee(request)}", null)
            session.answerFrame().also {
                journal.record(if (it == null) "NCALayer: no answer" else "NCALayer: answer received", null)
            }
        } finally {
            session.cancel()
        }
    }

    /** Ответ по делу из очереди кадров; приветствие с версией ответом не считается. */
    private suspend fun DefaultClientWebSocketSession.answerFrame(): JsonObject? {
        while (true) {
            val text = (incoming.receive() as? Frame.Text)?.readText() ?: continue
            val frame = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            val fields = frame?.keys?.joinToString(" ") ?: "not JSON"
            journal.record("NCALayer: frame of ${text.length} chars, fields: $fields", null)
            if (frame != null && !frame.isGreeting()) return frame
        }
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

/** Ушёл ли запрос и сколько прошло с тех пор. */
internal class SentMark {
    var mark: TimeMark? = null
    val done: Boolean get() = mark != null
    val waited: Duration get() = mark?.elapsedNow() ?: Duration.ZERO
}

/** Клиент вебсокета к петле: на каждый обмен свой, закрывается с обменом. */
internal fun ncaClient(): HttpClient = HttpClient(CIO) {
    install(WebSockets)
    engine {
        https {
            trustManager = LocalhostTrust
            random = SecureRandom()
        }
    }
}

/**
 * Доверие соединению с петлёй.
 *
 * Сертификат NCALayer самоподписан и меняется при переустановке; проверять
 * его цепочку не у кого. Собеседник опознан адресом `127.0.0.1`, дальше
 * этого соединения доверие не идёт.
 */
private object LocalhostTrust : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
