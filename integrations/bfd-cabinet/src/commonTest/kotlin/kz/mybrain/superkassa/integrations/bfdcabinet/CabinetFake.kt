package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf

/**
 * Подставной кабинет: отвечает заданными телами и запоминает запросы.
 *
 * Вход по ЭЦП он проводит сам — задача, подпись, доступ `access-1`, — и
 * запросы входа в [asked] не попадают: проверки смотрят на ручки сценария.
 *
 * @param login ответ на вход по подписи.
 * @param answer ответ на запрос сценария; `null` — `404`.
 */
internal class CabinetFake(
    private val login: Reply = Reply(LOGIN),
    private val answer: (HttpRequestData) -> Reply?
) {

    /** Запросы сценария по порядку. */
    val asked = mutableListOf<HttpRequestData>()

    /** Подписано подставным подписывающим. */
    val signed = mutableListOf<String>()

    val engine = MockEngine { request ->
        val reply = signIn(request) ?: answer(request.also { asked += it }) ?: Reply(NOTHING, NOT_FOUND)
        respond(reply.body, HttpStatusCode.fromValue(reply.status), jsonHeaders)
    }

    /** Кабинет, в который владелец уже вошёл. */
    suspend fun cabinet(settings: CabinetSettings = CabinetSettings()): BfdCabinet =
        unsigned(settings).also { it.account.signIn() }

    /** Кабинет, в который ещё не входили. */
    fun unsigned(settings: CabinetSettings = CabinetSettings()): BfdCabinet =
        BfdCabinet(settings, signer = { payload -> "cms-of-$payload".also { signed += it } }, engine = engine)

    private fun signIn(request: HttpRequestData): Reply? = when (request.url.encodedPath) {
        "/api/auth/eds/challenge" -> Reply(CHALLENGE)
        "/api/auth/eds" -> login
        else -> null
    }

    /** Ответ подставного кабинета. */
    data class Reply(val body: String, val status: Int = OK)

    companion object {
        const val OK = 200
        const val NOT_FOUND = 404
        const val NOTHING = """{"title":"Not Found"}"""
        const val CHALLENGE = """{"challengeId":"c-1","payload":"cGF5bG9hZA==","expiresAt":"2026-09-07T19:00:00Z"}"""
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        const val LOGIN = """{"accessToken":"access-1","expiresAt":"2026-09-17T20:24:20Z",
            "user":{"id":"987afff6","iin":"900101300000","fullName":"Курманов Азамат Бахытжанович"},
            "company":{"id":"939158cd","bin":"230140000000","name":"ТОО Азик и Ко"}}"""

        /** Кабинет, отвечающий на всё одним телом. */
        fun always(body: String, status: Int = OK) = CabinetFake { Reply(body, status) }
    }
}

/** Тело запроса текстом. */
internal fun HttpRequestData.text(): String = (body as? TextContent)?.text.orEmpty()

/** Путь со строкой запроса, как его увидел кабинет. */
internal fun HttpRequestData.target(): String =
    url.encodedPath + (url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: "")
