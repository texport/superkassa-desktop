package kz.mybrain.superkassa.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogJournal
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.eds.EdsProblem
import kz.mybrain.superkassa.desktop.eds.EdsRefusal
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Обмен с NCALayer живьём — на подставном вебсокете.
 *
 * Проверяется то, из-за чего вход по ЭЦП простоял три минуты и кончился
 * неверным объяснением: молчание после принятого запроса, отмена
 * владельцем, отказ NCALayer и запасной путь к прежнему модулю.
 * Живой NCALayer тут не нужен — ключа у приложения нет, а разговор
 * ключа не касается.
 */
class EdsExchangeTest {

    private val payload = "cGF5bG9hZA=="

    private fun layer(fake: NcaFake, window: Duration = 1.seconds) = NcaLayer(fake.address, window)

    /**
     * Соединение поднялось, запрос ушёл, ответа нет — и это не «запустите
     * NCALayer»: он запущен и запрос принял. Владелец читал обратное
     * спустя три минуты после того, как сам подписал в окне NCALayer.
     */
    @Test
    fun `молчание NCALayer названо молчанием, а не его отсутствием`() {
        NcaFake { NcaReply.Silence }.use { fake ->
            val refusal = assertFailsWith<EdsRefusal> { runBlocking { layer(fake).signCms(payload) } }

            assertEquals(EdsProblem.Declined, refusal.problem)
            assertEquals(NcaLayer.NO_ANSWER, refusal.detail)
        }
    }

    /**
     * NCALayer не запущен — вот это и есть «запустите его».
     *
     * Отказ приходит за рукопожатие, а не за три минуты: ждать окно
     * подписи от того, кто не отвечает на подключение, незачем.
     */
    @Test
    fun `отсутствие NCALayer видно по рукопожатию`() {
        val refusal = assertFailsWith<EdsRefusal> {
            runBlocking { NcaLayer("ws://127.0.0.1:$FREE_PORT", 1.seconds).signCms(payload) }
        }

        assertEquals(EdsProblem.Unreachable, refusal.problem)
        assertEquals(NcaLayer.NO_HANDSHAKE, refusal.detail)
    }

    /**
     * Старые выпуски NCALayer модуля `basics` не знают: они закрывают
     * соединение, не показав окна. Запасной путь к прежнему модулю
     * заведён ровно для этого, а срабатывал только на явный отказ —
     * при молчании подпись не получалась вовсе.
     */
    @Test
    fun `при молчании нового модуля подпись берётся у прежнего`() {
        NcaFake { request ->
            if (request.contains(BASICS)) NcaReply.Closed() else NcaReply.Frames(listOf(LEGACY_SIGNED))
        }.use { fake ->
            assertEquals("MIIC-legacy", runBlocking { layer(fake).signCms(payload) })
            assertTrue(fake.asked.first().contains(BASICS), "первым спрашивается новый модуль")
            assertTrue(fake.asked.last().contains(LEGACY), "прежний модуль не спросили: ${fake.asked.size}")
        }
    }

    /** Отказ NCALayer доходит его словами, и второго окна за ним нет. */
    @Test
    fun `отказ владельца не повторяется вторым окном`() {
        NcaFake { NcaReply.Frames(listOf(DECLINED)) }.use { fake ->
            val refusal = assertFailsWith<EdsRefusal> { runBlocking { layer(fake).signCms(payload) } }

            assertTrue(refusal.detail.contains("canceled"), "слова NCALayer: ${refusal.detail}")
            assertEquals(1, fake.asked.size, "после отказа владельца окно открылось второй раз")
        }
    }

    /**
     * Закрытое владельцем окно — его решение, и повторять нечего.
     *
     * От закрытия по незнакомому модулю оно отличается временем: быстрее
     * человек окно не закроет, потому что ещё не увидел его.
     */
    @Test
    fun `закрытое окно подписи не открывается второй раз`() {
        NcaFake { NcaReply.Closed(after = 4.seconds) }.use { fake ->
            val refusal = assertFailsWith<EdsRefusal> {
                runBlocking { layer(fake, window = 20.seconds).signCms(payload) }
            }

            assertEquals(NcaLayer.WINDOW_CLOSED, refusal.detail)
            assertEquals(1, fake.asked.size, "окно закрыли, а приложение открыло его снова")
        }
    }

    /**
     * Отмена владельца остаётся отменой: ни отказа на экране, ни повтора
     * прежним модулем. Прежде `runCatching` вокруг ожидания глотал её
     * наравне с отказами.
     */
    @Test
    fun `отмена владельцем остаётся отменой`() = runBlocking {
        NcaFake { NcaReply.Silence }.use { fake ->
            var refused: EdsRefusal? = null
            var signed: String? = null
            val signing = launch(Dispatchers.Default) {
                try {
                    signed = layer(fake, window = 1.minutes).signCms(payload)
                } catch (refusal: EdsRefusal) {
                    refused = refusal
                }
            }
            awaitRequest(fake)
            signing.cancelAndJoin()

            assertTrue(signing.isCancelled)
            assertNull(refused, "по отмене владельца отказ не показывается")
            assertNull(signed)
            assertEquals(1, fake.asked.size, "по отмене ничего не повторяется")
        }
    }

    /**
     * Обмен виден в журнале на обычном уровне.
     *
     * Прежде между запросом задачи у кабинета и отказом в журнале не было
     * ни одной строки: чем кончился обмен, нельзя было понять ни владельцу,
     * ни поддержке. Подписи и содержимого для подписи в журнале при этом
     * быть не должно — владелец пересылает его целиком.
     */
    @Test
    fun `обмен записан в журнал, а подпись и содержимое в него не попали`() {
        val was = AppLog.journal
        AppLog.journal = LogJournal(level = LogLevel.Info)
        try {
            NcaFake { NcaReply.Frames(listOf(SIGNED)) }.use { fake ->
                runBlocking { layer(fake).signCms(payload) }
            }
            val lines = AppLog.entries.map { it.text }

            assertTrue(lines.any { it.contains("рукопожатие прошло") }, "$lines")
            assertTrue(lines.any { it.contains("$BASICS/sign") }, "модуль и метод: $lines")
            assertTrue(lines.any { it.contains("поля: status body") }, "состав кадра: $lines")
            assertTrue(lines.any { it.contains("ответ получен") }, "исход обмена: $lines")
            assertFalse(lines.any { it.contains("MIIC-signature") }, "подпись в журнале: $lines")
            assertFalse(lines.any { it.contains(payload) }, "содержимое для подписи в журнале: $lines")
        } finally {
            AppLog.journal = was
        }
    }

    /** Ждёт, пока запрос дойдёт до подставного NCALayer. */
    private suspend fun awaitRequest(fake: NcaFake) = withTimeout(WAIT) {
        while (fake.asked.isEmpty()) delay(STEP)
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
