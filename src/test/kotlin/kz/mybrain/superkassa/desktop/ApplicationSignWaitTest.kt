package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCompany
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetMe
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetUser
import kz.mybrain.superkassa.desktop.ui.cabinet.ActionKind
import kz.mybrain.superkassa.desktop.ui.cabinet.DeregistrationReason
import kz.mybrain.superkassa.desktop.ui.cabinet.RegistrationActionsBlock
import kz.mybrain.superkassa.desktop.ui.cabinet.submitApplication
import kz.mybrain.superkassa.desktop.ui.setup.ApplicationStepCard
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Ожидание подписи при подаче заявления — в кабинете и в мастере.
 *
 * Видимый срок с отменой стоял только на двери входа, а подпись приложение
 * просит в трёх местах: в двух остальных владелец смотрел на занятую кнопку.
 * Главное здесь — что отменённая подача ничего не отправляет в кабинет.
 */
class ApplicationSignWaitTest {

    private val texts = cabinetTexts(Language.Ru)
    private val setup = setupTexts(Language.Ru)

    /** Куда дошли обращения к кабинету: по ним видно, ушло ли подписанное. */
    private val asked = CopyOnWriteArrayList<String>()

    /** Подача из кабинета: на месте кнопки отсчёт срока и отмена. */
    @Test
    fun `подача из кабинета показывает ожидание подписи`() {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = cabinet(fake)
            RenderProbe(CARD, TALL) {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                    RegistrationActionsBlock(session(), cabinet, texts, draft()) {}
                }
            }.use { probe ->
                val before = probe.frame()
                probe.click(SUBMIT)
                val waiting = probe.frame()
                File("/tmp/fix-15-cabinet-wait.png").writeBytes(waiting)

                assertFalse(waiting.contentEquals(before), "ожидание подписи на экране не показано")
                Thread.sleep(TICK.inWholeMilliseconds)
                assertTrue(probe.changedFrom(waiting), "отсчёт не двигается")

                // Отмена возвращает кнопку подачи: оставленный отсчёт значил бы,
                // что владелец ждёт того, чего никто уже не делает.
                probe.click(CANCEL)
                val cancelled = probe.frame()
                File("/tmp/fix-15-cabinet-cancelled.png").writeBytes(cancelled)
                assertFalse(cancelled.contentEquals(waiting), "отсчёт остался на экране после отмены")
                assertFalse(cabinet.busy, "после отмены приложение осталось занятым")
            }
        }
    }

    /** Тот же отсчёт в мастере подключения кассы. */
    @Test
    fun `мастер показывает то же ожидание подписи`() {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = cabinet(fake)
            RenderProbe(CARD, TALL) { Step(cabinet) }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                val before = probe.frame()
                probe.click(STEP_SUBMIT)
                val waiting = probe.frame()
                File("/tmp/fix-15-setup-wait.png").writeBytes(waiting)

                assertFalse(waiting.contentEquals(before), "ожидание подписи в мастере не показано")
                Thread.sleep(TICK.inWholeMilliseconds)
                assertTrue(probe.changedFrom(waiting), "отсчёт в мастере не двигается")
            }
        }
    }

    /**
     * Отмена ожидания не отправляет заявление: подпись стоит между
     * подготовкой и отправкой, и в КГД к этому мигу ещё ничего не ушло.
     */
    @Test
    fun `отменённая подача ничего не отправляет в кабинет`() = runBlocking {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = cabinet(fake)
            val submitting = launch(Dispatchers.Default) {
                submitApplication(cabinet, ActionKind.Registration, "r-1", "", REASON, "")
            }
            withTimeout(WAIT) { while (fake.asked.isEmpty()) delay(STEP) }
            submitting.cancelAndJoin()

            assertTrue(asked.any { it.endsWith("/registration/application") }, "заявление не готовилось: $asked")
            assertFalse(asked.any { it.endsWith("/registration/sign") }, "подписанное ушло в кабинет: $asked")
            assertFalse(cabinet.busy, "после отмены приложение осталось занятым")
        }
    }

    /** Шаг постановки на учёт так, как его собирает мастер подключения. */
    @Composable
    private fun Step(cabinet: CabinetSession) {
        val session = session()
        val started = remember { KkmSetupDraft(session.preferences).apply { rememberRegister("r-1", 5_000_021) } }
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) { ApplicationStepCard(session, cabinet, setup, started) {} }
    }

    /** Касса-черновик: ей подаётся постановка на учёт. */
    private fun draft() = CabinetRegister(
        id = "r-1",
        kkmId = 5_000_021,
        internalName = "Касса у входа",
        status = "DRAFT",
        factoryNumber = "SN-ECC-172758"
    )

    private fun session(): Session {
        val http = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeader) })
        val directory = Files.createTempDirectory("sign-wait").toFile()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
            .also { it.switchLanguage(Language.Ru) }
    }

    /** Кабинет готовит заявление, а подписывает подставной NCALayer — молчанием. */
    private fun cabinet(fake: NcaFake): CabinetSession {
        val engine = MockEngine { request ->
            asked += request.url.encodedPath
            respond(PREPARED, HttpStatusCode.OK, jsonHeader)
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetSession(CabinetClient(http = http), NcaLayer(fake.address, 1.minutes)).also {
            it.access.enter(ACCESS, CabinetMe(user = OWNER, company = COMPANY))
        }
    }

    private companion object {
        val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
        const val ACCESS = "sign-wait-access"
        val OWNER = CabinetUser(id = "u-1", iin = "900101300000", fullName = "Курманов Азамат Бахытжанович")
        val COMPANY = CabinetCompany(id = "c-1", bin = "230140000000", name = "ТОО «Азик и Ко»")
        val REASON = DeregistrationReason.CessationOfUse
        const val PREPARED = """{"actionId":"a-1","actionType":"REGISTRATION","payloadToSign":"cGF5bG9hZA=="}"""

        /** Где на карточке стоит «Подать заявление» — в кабинете и в мастере. */
        val SUBMIT = Offset(90f, 90f)
        val STEP_SUBMIT = Offset(120f, 120f)

        /** Где стоит «Отменить ожидание» в карточке заявлений кабинета. */
        val CANCEL = Offset(360f, 188f)

        const val CARD = 720
        const val TALL = 420
        const val SETTLE = 30
        val TICK = 1200.milliseconds
        val WAIT = 10.seconds
        val STEP = 20.milliseconds
    }
}
