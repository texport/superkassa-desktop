package kz.mybrain.superkassa.data.analytics

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.integrations.bfdcabinet.BfdCabinet
import kz.mybrain.superkassa.kassa.SilentJournal

/**
 * Кабинет модуля на подставных ответах — с уже вошедшим владельцем.
 *
 * Ответ выбирается по пути со строкой запроса так, как его увидел кабинет;
 * `null` — `404`, как у кабинета, в котором ручки ещё нет. Вход по подписи
 * проходит здесь же и в [asked] не попадает: проверки смотрят на ручки
 * аналитики.
 */
class CabinetReplies(private val reply: (String) -> CabinetReply?) {

    /** Ручки аналитики по порядку, с их строкой запроса. */
    val asked = mutableListOf<String>()

    private val engine = MockEngine { request ->
        val query = request.url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty()
        val answer = signIn(request.url.encodedPath)
            ?: reply((request.url.encodedPath + query).also { asked += it })
            ?: CabinetReply(NOT_FOUND, HttpStatusCode.NotFound.value)
        respond(answer.body, HttpStatusCode.fromValue(answer.status), JSON)
    }

    /** Кабинет, в который владелец уже вошёл. */
    val cabinet: BfdCabinet = BfdCabinet(signer = { payload -> "cms-of-$payload" }, engine = engine)
        .also { runBlocking { it.account.signIn() } }

    /** Аналитика приложения поверх этого кабинета. */
    val analytics: CabinetAnalytics = CabinetAnalytics(cabinet, SilentJournal)

    private fun signIn(path: String): CabinetReply? = when (path) {
        "/api/auth/eds/challenge" -> CabinetReply(CHALLENGE)
        "/api/auth/eds" -> CabinetReply(LOGIN)
        else -> null
    }

    companion object {
        /** Кабинет, отвечающий на всё одним телом. */
        fun always(body: String, status: Int = HttpStatusCode.OK.value) = CabinetReplies { CabinetReply(body, status) }

        /** Кабинет, отвечающий телом по пути; `null` — `404`. */
        fun answering(reply: (String) -> String?) = CabinetReplies { target -> reply(target)?.let(::CabinetReply) }

        const val NOT_FOUND = """{"code":"NOT_FOUND","detail":"no such","status":404,"title":"Not Found"}"""
        private val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        private const val CHALLENGE =
            """{"challengeId":"c-1","payload":"cGF5bG9hZA==","expiresAt":"2026-09-07T19:00:00Z"}"""
        private const val LOGIN = """{"accessToken":"access-1","expiresAt":"2026-09-17T20:24:20Z",
            "user":{"id":"u-1","iin":"920313351246","fullName":"Иванов Сергей"},
            "company":{"id":"c-1","bin":"920313351246","name":"ТОО «Сеть касс»"}}"""
    }
}

/** Ответ подставного кабинета: тело и состояние. */
data class CabinetReply(val body: String, val status: Int = HttpStatusCode.OK.value)
