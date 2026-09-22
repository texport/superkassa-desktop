package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.users.UsersScreen
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Удалив себя, администратор выходит, а не остаётся за кассой с мёртвым пином.
 *
 * Второй администратор на кассе есть, и удалить себя правила не мешают —
 * так уходит из компании тот, кто её заводил. Но узел вместе с кассиром
 * забывает и его пин: рабочее место продолжало показывать вошедшего
 * и отвечало отказом на каждое следующее действие, ни разу не сказав,
 * что кассира больше нет.
 */
class UsersRemoveSelfTest {

    private val asked = CopyOnWriteArrayList<String>()

    @Test
    fun `удаливший себя администратор выходит из кассы`() {
        val session = workplace()
        RenderProbe(width = WIDE, height = TALL) { UsersScreen(session) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(REMOVE_OWN_ROW)
            File("/tmp/audit-users-remove-self-ask.png").writeBytes(probe.frame())
            probe.click(CONFIRM)
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-remove-self-done.png").writeBytes(probe.frame())

            assertTrue(asked.any { it.startsWith("DELETE") }, "кассира так и не удалили: $asked")
            assertFalse(session.signedIn, "удалённый кассир остался за кассой")
        }
    }

    /** Узел с двумя администраторами: удалить себя правила не мешают. */
    private fun workplace(): Session {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            asked += "${request.method.value} $path"
            when {
                path.endsWith("/users/me") -> answer(ME)
                request.method == HttpMethod.Delete -> answer("{}")
                path.endsWith("/users") -> answer(LIST)
                else -> respondError(HttpStatusCode.ServiceUnavailable)
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val home = Files.createTempDirectory("users-remove-self").toFile()
        val session = Session(ServerClient(http = http), Preferences(File(home, "kkm")))
        runBlocking { session.signIn(KassaScene.kkm(), PIN) }
        return session
    }

    private fun MockRequestHandleScope.answer(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40
        const val PIN = "1234"

        const val ME = """{"userId":"u-1","name":"Айгүл Сәрсенова","role":"ADMIN"}"""
        const val LIST = """[{"userId":"u-1","name":"Айгүл Сәрсенова","role":"ADMIN"},
            {"userId":"u-2","name":"Асхат Нұрланов","role":"ADMIN"}]"""

        /** Где в своей строке стоит корзина и где «Удалить» в вопросе. */
        val REMOVE_OWN_ROW = Offset(1316f, 343f)
        val CONFIRM = Offset(899f, 525f)
    }
}
