package kz.mybrain.superkassa.desktop.eds

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Обмен с NCALayer в журнале.
 *
 * Прежде между запросом задачи у кабинета и отказом подписи в журнале
 * не было ни одной строки: понять, дошёл ли запрос до NCALayer и чем
 * кончился обмен, было нельзя — а разбирают это по журналу с чужой
 * машины, без отладочного режима. Поэтому уровень обычный.
 *
 * В журнал идут только адрес, имя модуля с методом, состав полей кадра
 * и его длина. Ни подписи, ни содержимого для подписи, ни сертификата,
 * ни полей ключа там нет: журнал владелец пересылает в поддержку целиком.
 */
internal fun ncaJournal(what: String) =
    AppLog.record(LogSource.Signature, LogLevel.Info, "$NCA: $what")

/** Сорванный шаг: имя исключения — единственная зацепка при разборе. */
internal fun ncaJournalBroken(what: String, failure: Throwable) = AppLog.record(
    source = LogSource.Signature,
    level = LogLevel.Warning,
    text = "$NCA: $what не прошло, ${failure::class.simpleName}"
)

/**
 * Кому ушёл запрос: модуль и метод.
 *
 * Имя модуля с методом — не тайна, и это единственное, по чему потом
 * видно, о чём приложение просило NCALayer: само содержимое для подписи
 * в журнал не идёт.
 */
internal fun ncaAddressee(request: JsonObject): String =
    ADDRESSEE.mapNotNull { (request[it] as? JsonPrimitive)?.content }.joinToString("/")

/** Пришедший кадр: только состав полей верхнего уровня и длина. */
internal fun ncaJournalFrame(frame: JsonObject?, length: Int) = ncaJournal(
    "кадр $length знаков, поля: ${frame?.keys?.joinToString(" ") ?: "не JSON"}"
)

/** Соединение с NCALayer на петле. */
internal fun ncaClient(): HttpClient = HttpClient(CIO) {
    install(WebSockets)
    engine {
        https {
            trustManager = LocalhostTrust
            random = SecureRandom()
        }
    }
}

/** Чем подписаны строки обмена в журнале. */
private const val NCA = "NCALayer"

/** Поля запроса, называющие того, кому он адресован. */
private val ADDRESSEE = listOf("module", "method")

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
