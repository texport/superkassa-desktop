package kz.mybrain.superkassa.desktop.server.releases

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.app.log.logged
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

/**
 * Выпуск кассы, как его описывает GitHub.
 *
 * Из ответа берётся только то, что нужно проверке: метка, страница
 * выпуска и файлы установщиков. Остальные поля ответа не читаются.
 */
@Serializable
data class Release(
    @SerialName("tag_name") val tag: String,
    @SerialName("html_url") val page: String,
    val assets: List<ReleaseAsset> = emptyList()
)

/** Файл выпуска: установщик под одну из систем. */
@Serializable
data class ReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String
)

/** Чем закончился вопрос о свежем выпуске. */
sealed interface ReleaseAnswer {
    data class Found(val release: Release) : ReleaseAnswer

    /** GitHub не ответил или ответил не выпуском; причина — для журнала. */
    data class Unreachable(val reason: String) : ReleaseAnswer
}

/**
 * Свежий выпуск кассы на GitHub.
 *
 * Выпуски публичные, и GitHub отдаёт их без ключа. Клиент не решает,
 * новее ли выпуск установленной кассы, — только приносит его; сравнение
 * и выбор установщика делает рабочее место.
 */
class ReleaseClient(
    private val http: HttpClient = defaultHttpClient(),
    private val url: String = LATEST_URL
) {

    suspend fun latest(): ReleaseAnswer {
        val response = try {
            logged(LogSource.App, HttpMethod.Get, url, body = null) { http.get(url) }
        } catch (failure: IOException) {
            return ReleaseAnswer.Unreachable(reasonOf(failure))
        } catch (failure: UnresolvedAddressException) {
            return ReleaseAnswer.Unreachable(reasonOf(failure))
        }
        if (!response.status.isSuccess()) {
            return ReleaseAnswer.Unreachable("HTTP ${response.status.value}")
        }
        return try {
            ReleaseAnswer.Found(json.decodeFromString<Release>(response.bodyAsText()))
        } catch (failure: SerializationException) {
            ReleaseAnswer.Unreachable("ответ не о выпуске: ${failure.message}")
        }
    }

    fun close() = http.close()

    /** Причина для журнала: слова исключения, а без них — его имя. */
    private fun reasonOf(failure: Exception): String = failure.message ?: failure::class.simpleName.orEmpty()

    companion object {
        /** Последний выпуск настольной кассы. */
        const val LATEST_URL: String = "https://api.github.com/repos/texport/superkassa-desktop/releases/latest"

        /**
         * Сколько ждать GitHub.
         *
         * Проверка идёт в стороне от работы кассира и торопить её незачем,
         * но и держать соединение без конца нельзя: за прокси без интернета
         * оно висело бы до закрытия кассы.
         */
        private const val WAIT_MS = 15_000L

        val json: Json = Json { ignoreUnknownKeys = true }

        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = WAIT_MS
                requestTimeoutMillis = WAIT_MS
                socketTimeoutMillis = WAIT_MS
            }
        }
    }
}
