package kz.mybrain.superkassa.integrations.releases

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kz.mybrain.superkassa.integrations.releases.wire.GithubRelease
import kz.mybrain.superkassa.integrations.releases.wire.githubJson
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Последний выпуск кассы на GitHub.
 *
 * Выпуски публичные, и GitHub отдаёт их без ключа. Клиент не решает,
 * новее ли выпуск установленной кассы, — только приносит его: сравнение
 * версий и установка — дело приложения.
 *
 * @param source чьи выпуски спрашивать и сколько ждать.
 * @param engine движок Ktor; по умолчанию — движок платформы.
 */
class GithubReleases(
    private val source: ReleaseSource = ReleaseSource(),
    engine: HttpClientEngine = platformEngine()
) : AutoCloseable {
    private val http = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = source.wait.inWholeMilliseconds
            requestTimeoutMillis = source.wait.inWholeMilliseconds
            socketTimeoutMillis = source.wait.inWholeMilliseconds
        }
    }

    /**
     * Последний выпуск: без черновиков и предварительных выпусков.
     *
     * Сбой сети, отказ GitHub и ответ не о выпуске не бросаются, а
     * возвращаются [ReleaseAnswer.Unreachable]: проверка выпусков идёт
     * в стороне от работы кассира и прерывать её не должна.
     */
    suspend fun latest(): ReleaseAnswer {
        val response = try {
            http.get(source.latestUrl) {
                header(HttpHeaders.Accept, GITHUB_JSON)
                header(HttpHeaders.UserAgent, source.userAgent)
            }
        } catch (failure: IOException) {
            return ReleaseAnswer.Unreachable(failure.message ?: failure::class.simpleName.orEmpty())
        }
        return answerOf(response)
    }

    /** Закрывает соединение с GitHub. */
    override fun close() = http.close()

    private suspend fun answerOf(response: HttpResponse): ReleaseAnswer {
        if (!response.status.isSuccess()) return ReleaseAnswer.Unreachable("HTTP ${response.status.value}")
        return try {
            ReleaseAnswer.Found(githubJson.decodeFromString<GithubRelease>(response.bodyAsText()).release())
        } catch (failure: SerializationException) {
            ReleaseAnswer.Unreachable("not a release: ${failure::class.simpleName}")
        }
    }

    private companion object {
        /** Вид ответа, который GitHub обещает не менять. */
        const val GITHUB_JSON = "application/vnd.github+json"
    }
}

/**
 * Чьи выпуски спрашивать.
 *
 * @property owner владелец репозитория на GitHub.
 * @property repository имя репозитория.
 * @property api основание GitHub API: у GitHub Enterprise оно своё.
 * @property userAgent чем приложение представляется: GitHub отвергает
 *   запросы без имени.
 * @property wait сколько ждать ответа. Проверка идёт в стороне от работы
 *   кассира, но без срока за прокси без интернета висела бы до закрытия кассы.
 */
data class ReleaseSource(
    val owner: String = "texport",
    val repository: String = "superkassa-desktop",
    val api: String = "https://api.github.com",
    val userAgent: String = "Superkassa",
    val wait: Duration = 15.seconds
) {
    /** Адрес последнего выпуска. */
    val latestUrl: String get() = "${api.trimEnd('/')}/repos/$owner/$repository/releases/latest"
}
