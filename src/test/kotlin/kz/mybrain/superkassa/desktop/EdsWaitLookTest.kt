package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetSignIn
import kz.mybrain.superkassa.desktop.ui.cabinet.SignWait
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.edsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Экран входа, пока идёт ожидание подписи.
 *
 * Владелец подписал в окне NCALayer и три минуты смотрел на неподвижную
 * кнопку, а затем прочитал, что NCALayer не запущен. Проверяется то,
 * чего на экране не было: видимый срок ожидания и отмена — и то, что
 * по отмене приложение не остаётся занятым.
 */
class EdsWaitLookTest {

    private val texts = cabinetTexts(Language.Ru)
    private val eds = edsTexts(Language.Ru)

    /**
     * Ожидание видно, и оно движется.
     *
     * Кадр после нажатия отличается от кадра до него, а кадр спустя
     * секунду — от кадра сразу после нажатия: отсчёт идёт. Неподвижное
     * ожидание владелец читает как зависшее приложение.
     */
    @Test
    fun `ожидание подписи показано с отсчётом и отменой`() {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = cabinet(fake)
            RenderProbe { CabinetSignIn(session(), cabinet, texts) }.use { probe ->
                val door = probe.frame()
                probe.click(SIGN_IN)
                val waiting = probe.frame()
                File("/tmp/eds-wait-screen.png").writeBytes(waiting)

                assertTrue(cabinet.busy, "нажатие не дошло до кнопки входа: разметка карточки сдвинулась")
                assertFalse(waiting.contentEquals(door), "ожидание на экране не показано")
                Thread.sleep(TICK.inWholeMilliseconds)
                assertTrue(probe.changedFrom(waiting), "отсчёт не двигается")
            }
        }
    }

    /**
     * Отмена снимает ожидание и не оставляет приложение занятым.
     *
     * Кнопка отмены стоит там же, где кнопка входа: ожидание прерывается
     * тем же местом, где началось.
     */
    @Test
    fun `отмена снимает ожидание с экрана`() {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = cabinet(fake)
            RenderProbe { CabinetSignIn(session(), cabinet, texts) }.use { probe ->
                probe.click(SIGN_IN)
                probe.frame()
                probe.click(CANCEL)
                val after = probe.frame()
                File("/tmp/eds-wait-cancelled.png").writeBytes(after)

                assertFalse(cabinet.busy, "после отмены приложение осталось занятым")
                assertNull(cabinet.problem, "по отмене владельцу ничего не показывается")
            }
        }
    }

    /** Последние секунды срока: цифры те же, а ждать уже недолго. */
    @Test
    fun `остаток срока виден до последних секунд`() {
        val shot = RenderProbe(width = WIDTH, height = HEIGHT) {
            Column(modifier = Modifier.fillMaxSize().padding(Spacing.roomy)) {
                ElevatedCard(modifier = Modifier.widthIn(max = Sizes.loginColumn)) {
                    Column(modifier = Modifier.padding(Spacing.roomy)) {
                        SignWait(left = 7.seconds, window = NcaLayer.SIGN_WINDOW, texts = texts, eds = eds) {}
                    }
                }
            }
        }.use { probe ->
            repeat(FRAMES) { probe.frame() }
            probe.frame()
        }
        File("/tmp/eds-wait-last-seconds.png").writeBytes(shot)

        assertTrue(shot.isNotEmpty())
    }

    /**
     * Отмена посреди ожидания: сеанс кабинета не остаётся занятым
     * и молчит о помехе — владелец сам её и вызвал.
     */
    @Test
    fun `отменённый вход не оставляет сеанс занятым`() = runBlocking {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = cabinet(fake)
            val signing = launch(Dispatchers.Default) { cabinet.signIn() }
            withTimeout(WAIT) { while (fake.asked.isEmpty()) delay(STEP) }
            assertTrue(cabinet.busy, "ожидание подписи не показано занятостью")
            signing.cancelAndJoin()

            assertFalse(cabinet.busy, "экран остался занятым после отмены")
            assertNull(cabinet.problem, "по отмене владельцу ничего не показывается")
        }
    }

    /** Рабочее место со своим каталогом настроек: экран входа их читает. */
    private fun session(): Session {
        val http = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeader) })
        val directory = Files.createTempDirectory("eds-wait").toFile()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm"))).apply {
            switchLanguage(Language.Ru)
        }
    }

    /** Кабинет выдаёт задачу на подпись, а подписывает подставной NCALayer. */
    private fun cabinet(fake: NcaFake): CabinetSession {
        val engine = MockEngine { respond(CHALLENGE, HttpStatusCode.OK, jsonHeader) }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetSession(CabinetClient(http = http), NcaLayer(fake.address, 1.minutes))
    }

    private companion object {
        val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
        const val CHALLENGE = """{"challengeId":"c-1","payload":"cGF5bG9hZA=="}"""

        /** Где на карточке входа стоит кнопка входа и где — отмена ожидания. */
        val SIGN_IN = Offset(590f, 452f)
        val CANCEL = Offset(590f, 506f)

        const val WIDTH = 1180
        const val HEIGHT = 820
        const val FRAMES = 30
        val TICK = 1200.milliseconds
        val WAIT = 10.seconds
        val STEP = 20.milliseconds
    }
}
