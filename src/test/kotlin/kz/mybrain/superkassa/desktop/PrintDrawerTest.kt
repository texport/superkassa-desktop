package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.adoptKkms
import kz.mybrain.superkassa.desktop.app.log.LogJournal
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Кто рисует печатную форму, когда кассир не входил.
 *
 * Владелец открывает кабинет прямо с экрана входа и смотрит там чужие
 * чеки. Выбранной кассы у него нет и пина нет — прежде просмотр в этом
 * случае молча не делал ничего.
 */
class PrintDrawerTest {

    private fun kkm(id: String, name: String) = Kkm(kkmId = id, name = name)

    private fun session(): Session {
        val engine = MockEngine {
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        // Настройки свои: сеанс с настройками по умолчанию пишет в файл
        // рабочей кассы этой машины.
        val directory = Files.createTempDirectory("superkassa-drawer").toFile()
        directory.deleteOnExit()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
    }

    @Test
    fun `без вошедшего кассира рисует первая касса узла`() {
        val session = session()
        session.adoptKkms(listOf(kkm("a1", "Касса у входа"), kkm("b2", "Касса в зале")))

        val drawer = session.printDesk.drawer
        assertEquals("a1", drawer.kkm()?.kkmId, "рисовальщиком берётся первая касса списка")
    }

    @Test
    fun `выбранная касса сильнее первой в списке`() {
        val session = session()
        val second = kkm("b2", "Касса в зале")
        session.adoptKkms(listOf(kkm("a1", "Касса у входа"), second))
        session.select(second)

        assertEquals("b2", session.printDesk.drawer.kkm()?.kkmId)
    }

    @Test
    fun `узел не отдал ни одной кассы — об этом сказано словами`() {
        val session = session()

        assertNull(session.printDesk.drawer.kkm(), "рисовать нечем")
        assertNotNull(session.lastMessage, "молчание здесь — тот же дефект, что и пустой экран")
    }

    /**
     * Отказ называет связь, а не только следствие.
     *
     * Владелец смотрит документ кабинета и не знает, что печатную форму
     * по нему рисует касса на его же машине: «узел не отдал ни одной кассы»
     * читалось как нелогичность — при чём здесь узел.
     */
    @Test
    fun `отказ предпросмотра называет и кабинет, и кассу`() {
        val words = stringsOf(Language.Ru).preview.noDrawer
        assertTrue(words.contains("кабинет", ignoreCase = true), "не назван кабинет: $words")
        assertTrue(words.contains("касса", ignoreCase = true), "не названа касса: $words")
    }

    @Test
    fun `пина в сеансе нет — его спрашивают, назвав кассу`() {
        val session = session()
        session.adoptKkms(listOf(kkm("a1", "Касса у входа")))
        val drawer = session.printDesk.drawer

        var repeated = 0
        assertNull(drawer.resolve { repeated += 1 }, "без пина к узлу не ходим")
        val request = assertNotNull(drawer.request, "окно пина обязано открыться")
        assertEquals("Касса у входа", request.kkmTitle, "владелец должен видеть, чей пин спрашивают")
        assertEquals(0, repeated, "до ввода пина работа не повторяется")

        drawer.adopt("4827")
        assertNull(drawer.request, "окно закрывается вводом")
        assertEquals(1, repeated, "введённый пин продолжает ту же работу")
        assertEquals("a1" to "4827", drawer.resolve { }?.let { it.first.kkmId to it.second })
    }

    @Test
    fun `введённый пин не считается входом кассира`() {
        val session = session()
        session.adoptKkms(listOf(kkm("a1", "Касса у входа")))
        session.printDesk.drawer.resolve { }
        session.printDesk.drawer.adopt("4827")

        assertEquals("", session.pin, "пин кассира остаётся пустым")
        assertFalse(session.signedIn, "разделы кассы от этого не открываются")
    }

    @Test
    fun `отказ узла спрашивает пин заново, а не повторяет неверный`() {
        val session = session()
        session.adoptKkms(listOf(kkm("a1", "Касса у входа")))
        val drawer = session.printDesk.drawer
        drawer.resolve { }
        drawer.adopt("0000")

        drawer.refused { }
        assertNotNull(drawer.request, "владельцу дают ввести пин ещё раз")
        assertNull(drawer.resolve { }, "неверный пин забыт")
    }

    @Test
    fun `выход кассира забывает пин рисовальщика`() {
        val session = session()
        session.adoptKkms(listOf(kkm("a1", "Касса у входа")))
        session.printDesk.drawer.resolve { }
        session.printDesk.drawer.adopt("4827")

        session.signOut()
        assertNull(session.printDesk.drawer.request)
        assertNull(session.printDesk.drawer.resolve { }, "после выхода пин спрашивается заново")
    }

    /**
     * Пин уходит узлу заголовком `Authorization`, а журнал владелец
     * пересылает в поддержку целиком.
     */
    @Test
    fun `пин рисовальщика не попадает в журнал`() {
        val log = LogJournal(file = null, level = LogLevel.Debug) { MOMENT }
        log.record(
            source = LogSource.Node,
            level = LogLevel.Info,
            text = "POST /kkm/a1/documents/print.png",
            body = """{"authorization":"4827","pin":"4827"}"""
        )

        val written = log.entries.single().body.orEmpty()
        assertFalse(written.contains("4827"), "пин в журнале: $written")
    }

    private companion object {
        val MOMENT: LocalDateTime = LocalDateTime.of(2026, 9, 20, 10, 0, 0)
    }
}
