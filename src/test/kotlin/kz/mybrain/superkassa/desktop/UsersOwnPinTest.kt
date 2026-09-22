package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
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
import kotlin.test.Test
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Смена пина чужому кассиру не переселяет работу на его пин.
 *
 * Список кассиров перечитывается после смены пина, и, если прежний пин
 * его больше не берёт, экран пробует новый: так администратор, сменивший
 * пин самому себе, продолжает работу, а не упирается в отказ на удавшейся
 * операции. Но новый пин принадлежит тому кассиру, которому его задали.
 * Пока узел не ответил по любой причине, подстановка чужого пина
 * подписывала бы его именем чеки администратора.
 */
class UsersOwnPinTest {

    /** Чем подписано каждое обращение к узлу: по нему видно, чей пин ушёл. */
    private val asked = CopyOnWriteArrayList<String>()

    @Test
    fun `чужой пин не становится пином работающего`() {
        val session = workplace()
        RenderProbe(width = WIDE, height = TALL) { UsersScreen(session) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(NEW_PIN_OF_CASHIER)
            probe.type(NEW_PIN)
            File("/tmp/audit-users-pin-dialog.png").writeBytes(probe.frame())
            probe.click(CONFIRM)
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-pin-changed.png").writeBytes(probe.frame())

            assertTrue(asked.any { it.startsWith("PUT ") }, "пин кассиру так и не сменили: $asked")
            assertEquals(OWN_PIN, session.pin, "работа продолжилась под пином чужого кассира")
            assertFalse(asked.any { it.endsWith(" $NEW_PIN") }, "узел спрошен чужим пином: $asked")
        }
    }

    /**
     * Узел, который после смены пина перестал отвечать прежнему пину.
     *
     * Так выглядит любой перебой связи в миг между сменой пина и
     * перечитыванием списка: прежний пин не берёт, новый — берёт,
     * потому что принадлежит заведённому кассиру.
     */
    private fun workplace(): Session {
        var changed = false
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val pin = request.headers[HttpHeaders.Authorization]
            asked += "${request.method.value} $path $pin"
            when {
                path.endsWith("/users/me") -> answer(ME)
                request.method == HttpMethod.Put -> {
                    changed = true
                    answer("{}")
                }

                path.endsWith("/users") && (!changed || pin == NEW_PIN) -> answer(LIST)
                else -> respondError(HttpStatusCode.ServiceUnavailable)
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val home = Files.createTempDirectory("users-own-pin").toFile()
        val session = Session(ServerClient(http = http), Preferences(File(home, "kkm")))
        runBlocking { session.signIn(KassaScene.kkm(), OWN_PIN) }
        return session
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.answer(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40

        /** Пин, под которым работает администратор, и пин, который он задаёт кассиру. */
        const val OWN_PIN = "1234"
        const val NEW_PIN = "5555"

        const val ME = """{"userId":"u-1","name":"Айгүл Сәрсенова","role":"ADMIN"}"""
        const val LIST = """[{"userId":"u-1","name":"Айгүл Сәрсенова","role":"ADMIN"},
            {"userId":"u-2","name":"Дана Жумабаева","role":"CASHIER"}]"""

        /** Где в строке второго кассира стоит «Новый пин» и где «Сменить» в окне. */
        val NEW_PIN_OF_CASHIER = Offset(1231f, 459f)
        val CONFIRM = Offset(842f, 539f)
    }
}
