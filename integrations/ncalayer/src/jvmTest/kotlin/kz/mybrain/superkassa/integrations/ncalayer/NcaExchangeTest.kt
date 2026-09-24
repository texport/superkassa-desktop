package kz.mybrain.superkassa.integrations.ncalayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Обмен с NCALayer живьём — на подставном вебсокете.
 *
 * Проверяется то, из-за чего вход по ЭЦП простоял три минуты и кончился
 * неверным объяснением: молчание после принятого запроса, отмена,
 * отказ NCALayer и запасной путь к прежнему модулю.
 */
class NcaExchangeTest {

    private val payload = "cGF5bG9hZA=="

    private fun layer(fake: NcaFake, window: Duration = 1.seconds) =
        DesktopNcaLayer(NcaSettings(address = fake.address, signWindow = window))

    /**
     * Подпись отдаётся сразу, а не через срок ожидания: NCALayer закрывающего
     * кадра не присылает, и вежливое прощание ждало его до срока.
     */
    @Test
    fun signatureIsReturnedWithoutWaitingForGoodbye() {
        NcaFake { NcaReply.Frames(listOf(SIGNED)) }.use { fake ->
            val window = 30.seconds
            val took = TimeSource.Monotonic.markNow()

            assertEquals("MIIC-signature", runBlocking { layer(fake, window).sign(payload) })
            assertTrue(took.elapsedNow() < window / 2, "подпись пришла за ${took.elapsedNow()}")
        }
    }

    /** Окно подписи говорит на языке, выбранном в приложении сейчас, а не при запуске. */
    @Test
    fun signingWindowSpeaksTheCurrentLanguage() {
        NcaFake { NcaReply.Frames(listOf(SIGNED)) }.use { fake ->
            var language = "ru"
            val settings = NcaSettings(address = fake.address, signWindow = 1.seconds)
            val layer = DesktopNcaLayer(settings, locale = { language })
            runBlocking { layer.sign(payload) }
            language = "kk"
            runBlocking { layer.sign(payload) }

            val locales = fake.asked.map { Json.parseToJsonElement(it).jsonObject["args"]?.jsonObject?.get("locale") }
            assertEquals(listOf("ru", "kk"), locales.map { it?.jsonPrimitive?.content })
        }
    }

    /** Запрос принят, ответа нет — это не «запустите NCALayer»: он запущен. */
    @Test
    fun silenceIsNamedSilence() {
        NcaFake { NcaReply.Silence }.use { fake ->
            val refusal = assertFailsWith<NcaRefusal> { runBlocking { layer(fake).sign(payload) } }

            assertEquals(NcaReason.Silent, refusal.reason)
        }
    }

    /** Незапущенный NCALayer виден по рукопожатию, а не через три минуты. */
    @Test
    fun absentNcaLayerIsSeenAtHandshake() {
        val absent = DesktopNcaLayer(NcaSettings(address = "ws://127.0.0.1:$FREE_PORT", signWindow = 1.minutes))
        val took = TimeSource.Monotonic.markNow()
        val refusal = assertFailsWith<NcaRefusal> { runBlocking { absent.sign(payload) } }

        assertEquals(NcaReason.Unreachable, refusal.reason)
        assertTrue(took.elapsedNow() < 1.minutes / 2)
    }

    /** Выпуски, не знающие `basics`, закрывают соединение — подпись берётся у прежнего модуля. */
    @Test
    fun silentBasicsModuleFallsBackToLegacy() {
        NcaFake { request ->
            if (request.contains(BASICS)) NcaReply.Closed() else NcaReply.Frames(listOf(LEGACY_SIGNED))
        }.use { fake ->
            assertEquals("MIIC-legacy", runBlocking { layer(fake).sign(payload) })
            assertTrue(fake.asked.first().contains(BASICS), "первым спрашивается новый модуль")
            assertTrue(fake.asked.last().contains(LEGACY), "прежний модуль не спросили")
        }
    }

    @Test
    fun ownersRefusalIsNotRepeatedWithSecondWindow() {
        NcaFake { NcaReply.Frames(listOf(DECLINED)) }.use { fake ->
            val refusal = assertFailsWith<NcaRefusal> { runBlocking { layer(fake).sign(payload) } }

            assertTrue(refusal.detail.contains("canceled"), "слова NCALayer: ${refusal.detail}")
            assertEquals(1, fake.asked.size, "после отказа владельца окно открылось второй раз")
        }
    }

    /** Закрытое владельцем окно отличается временем: быстрее человек его не закроет. */
    @Test
    fun closedWindowIsNotOpenedAgain() {
        NcaFake { NcaReply.Closed(after = 4.seconds) }.use { fake ->
            val slow = layer(fake, window = 20.seconds)
            val refusal = assertFailsWith<NcaRefusal> { runBlocking { slow.sign(payload) } }

            assertEquals(NcaReason.WindowClosed, refusal.reason)
            assertEquals(1, fake.asked.size, "окно закрыли, а приложение открыло его снова")
        }
    }

    /** Отмена остаётся отменой: ни отказа, ни повтора прежним модулем. */
    @Test
    fun cancellationStaysCancellation() = runBlocking {
        NcaFake { NcaReply.Silence }.use { fake ->
            var refused: NcaRefusal? = null
            val signing = launch(Dispatchers.Default) {
                try {
                    layer(fake, window = 1.minutes).sign(payload)
                } catch (refusal: NcaRefusal) {
                    refused = refusal
                }
            }
            withTimeout(WAIT) { while (fake.asked.isEmpty()) delay(STEP) }
            signing.cancelAndJoin()

            assertTrue(signing.isCancelled)
            assertNull(refused, "по отмене отказ не показывается")
            assertEquals(1, fake.asked.size, "по отмене ничего не повторяется")
        }
    }

    /** Доверие без проверки сертификата — только петле. */
    @Test
    fun remoteAddressIsRejected() {
        val remote = NcaSettings(address = "wss://nca.example.kz:13579")
        assertFailsWith<IllegalArgumentException> { DesktopNcaLayer(remote) }
    }

    private companion object {
        const val BASICS = "kz.gov.pki.knca.basics"
        const val LEGACY = "kz.gov.pki.knca.commonUtils"
        const val SIGNED = """{"status":true,"body":{"result":["MIIC-signature"]}}"""
        const val LEGACY_SIGNED = """{"code":"200","result":"MIIC-legacy"}"""
        const val DECLINED = """{"status":false,"message":"action.canceled"}"""
        val WAIT = 10.seconds
        val STEP = 20.milliseconds

        /** Порт, который никто не слушает: занят на мгновение и отпущен. */
        val FREE_PORT = ServerSocket(0).use { it.localPort }
    }
}
