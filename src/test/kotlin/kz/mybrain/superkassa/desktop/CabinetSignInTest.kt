package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetSignIn
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Дверь в кабинет одна — по ЭЦП.
 *
 * Вход по набранным ИИН и БИН стоял на том же экране и предлагал ввести
 * любые двенадцать цифр: на экране входа это вторая, неохраняемая дверь,
 * и владелец видел её первым делом. Он остался средством отладки —
 * и виден только при включённой отладке.
 *
 * Проверяется кадром сцены, а не разбором дерева: спрятанный показ
 * не рисует ничего, и разница между двумя кадрами — единственное
 * доказательство, что поля действительно исчезли с экрана.
 */
class CabinetSignInTest {

    private val was = AppLog.debugMode

    @AfterTest
    fun restore() {
        AppLog.switchDebugMode(was)
    }

    private fun session(): Session {
        val http = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        // Свой каталог настроек: экран входа читает и пишет настройки,
        // и в общем временном каталоге это задело бы чужие.
        val directory = Files.createTempDirectory("signin").toFile()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
    }

    private fun door(debug: Boolean): ByteArray {
        AppLog.switchDebugMode(debug)
        val session = session()
        val cabinet = CabinetSession()
        return RenderProbe { CabinetSignIn(session, cabinet, cabinetTexts(Language.Ru)) }
            .use { probe ->
                val frame = probe.frame()
                File("/tmp/signin-${if (debug) "debug" else "plain"}.png").writeBytes(frame)
                frame
            }
    }

    @Test
    fun `без отладки на экране входа только ЭЦП`() {
        val plain = door(debug = false)
        val debug = door(debug = true)

        assertTrue(plain.isNotEmpty() && debug.isNotEmpty())
        assertTrue(!plain.contentEquals(debug), "поля ИИН и БИН видны и без режима отладки")
    }
}
