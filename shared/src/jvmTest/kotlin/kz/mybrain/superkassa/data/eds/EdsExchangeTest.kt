package kz.mybrain.superkassa.data.eds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.ncalayer.DesktopNcaLayer
import kz.mybrain.superkassa.integrations.ncalayer.NcaSettings
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
 * неверным объяснением: молчание после принятого запроса, отмена
 * владельцем, отказ NCALayer и запасной путь к прежнему модулю.
 * Живой NCALayer тут не нужен — ключа у приложения нет, а разговор
 * ключа не касается.
 */
class EdsExchangeTest {

    private val payload = "cGF5bG9hZA=="

    private fun layer(fake: NcaFake, window: Duration = 1.seconds) = signer(fake.address, window)

    private fun signer(address: String, window: Duration) =
        NcaSigner(DesktopNcaLayer(NcaSettings(address = address, signWindow = window)))

    /**
     * Подпись получена — и отдана сразу, а не через срок ожидания.
     *
     * NCALayer закрывающего кадра не присылает и держит соединение
     * открытым. Приложение прощалось вежливо и ждало его прощания:
     * владелец подписывал, подпись приходила — и через три минуты
     * он всё равно читал отказ, а подпись выбрасывалась.
     */
    @Test
    fun `подпись отдаётся сразу, не дожидаясь прощания NCALayer`() {
        NcaFake { NcaReply.Frames(listOf(SIGNED)) }.use { fake ->
            val window = 30.seconds
            val took = TimeSource.Monotonic.markNow()
            val signature = runBlocking { layer(fake, window).sign(payload) }

            assertEquals("MIIC-signature", signature)
            assertTrue(
                took.elapsedNow() < window / 2,
                "подпись пришла за ${took.elapsedNow()} — ждали прощания собеседника"
            )
        }
    }

    /**
     * Соединение поднялось, запрос ушёл, ответа нет — и это не «запустите
     * NCALayer»: он запущен и запрос принял. Владелец читал обратное
     * спустя три минуты после того, как сам подписал в окне NCALayer.
     */
    @Test
    fun `молчание NCALayer названо молчанием, а не его отсутствием`() {
        NcaFake { NcaReply.Silence }.use { fake ->
            val refusal = assertFailsWith<EdsRefusal> { runBlocking { layer(fake).sign(payload) } }

            assertEquals(EdsProblem.Declined, refusal.problem)
            assertEquals(Signer.NO_ANSWER, refusal.detail)
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
            runBlocking { signer("ws://127.0.0.1:$FREE_PORT", 1.seconds).sign(payload) }
        }

        assertEquals(EdsProblem.Unreachable, refusal.problem)
        assertEquals(Signer.NO_HANDSHAKE, refusal.detail)
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
            assertEquals("MIIC-legacy", runBlocking { layer(fake).sign(payload) })
            assertTrue(fake.asked.first().contains(BASICS), "первым спрашивается новый модуль")
            assertTrue(fake.asked.last().contains(LEGACY), "прежний модуль не спросили: ${fake.asked.size}")
        }
    }

    /** Отказ NCALayer доходит его словами, и второго окна за ним нет. */
    @Test
    fun `отказ владельца не повторяется вторым окном`() {
        NcaFake { NcaReply.Frames(listOf(DECLINED)) }.use { fake ->
            val refusal = assertFailsWith<EdsRefusal> { runBlocking { layer(fake).sign(payload) } }

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
                runBlocking { layer(fake, window = 20.seconds).sign(payload) }
            }

            assertEquals(Signer.WINDOW_CLOSED, refusal.detail)
            assertEquals(1, fake.asked.size, "окно закрыли, а приложение открыло его снова")
        }
    }

    /**
     * Отмена владельца остаётся отменой: ни отказа на экране, ни повтора
     * прежним модулем. Прежде `runCatching` вокруг ожидания глотал её
     * наравне с отказами.
     */
    @Test
    fun `отмена владельцем остаётся отменой`(): Unit = runBlocking {
        NcaFake { NcaReply.Silence }.use { fake ->
            var refused: EdsRefusal? = null
            var signed: String? = null
            val signing = launch(Dispatchers.Default) {
                try {
                    signed = layer(fake, window = 1.minutes).sign(payload)
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
