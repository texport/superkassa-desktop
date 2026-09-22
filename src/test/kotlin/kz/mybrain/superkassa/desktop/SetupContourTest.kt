package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.setup.AdminStepCard
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Касса не заводится в контур, которого владелец не выбирал.
 *
 * Контуры БФД приходят справочником узла. Пока справочник не прочитан,
 * выбирать не из чего, и подставлять в поле нечего — а кнопка всё равно
 * оживала от одного годного пина и отправляла заведение с пустым
 * контуром. Узел отвечал на это отказом, но токен кабинета к тому мигу
 * уже был выдан на эту кассу. Ручной путь подключения так не делает:
 * там заведение ждёт выбранного контура.
 */
class SetupContourTest {

    /** Куда дошли обращения к узлу: по ним видно, ушло ли заведение. */
    private val asked = CopyOnWriteArrayList<String>()

    @Test
    fun `без выбранного контура касса не заводится`() {
        val session = workplace()
        RenderProbe(width = WIDE, height = TALL) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                AdminStepCard(session, mockCabinet(ISSUED), setupTexts(Language.Ru), draft(session), true) {}
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(PIN_FIELD)
            probe.type(GOOD_PIN)
            probe.click(CONNECT)
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-contour-empty.png").writeBytes(probe.frame())

            assertTrue(
                asked.none { it.startsWith("POST") },
                "касса заведена без выбранного контура: $asked"
            )
        }
    }

    private fun draft(session: Session) = KkmSetupDraft(session.preferences).apply {
        rememberFactory("KZT26E2C509A200", "2026")
        rememberRegister("r-1", 5_000_021, "Касса у входа")
    }

    /** Узел, справочник контуров которого рабочее место так и не прочитало. */
    private fun workplace(): Session {
        val engine = MockEngine { request ->
            asked += "${request.method.value} ${request.url.encodedPath}"
            respondError(HttpStatusCode.ServiceUnavailable)
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val home = Files.createTempDirectory("setup-contour").toFile()
        return Session(ServerClient(http = http), Preferences(File(home, "kkm")))
    }

    private companion object {
        const val WIDE = 900
        const val TALL = 460
        const val SETTLE = 30
        const val GOOD_PIN = "4821"

        /** Кабинет отдаёт и карточку кассы, и выданный ей токен. */
        const val ISSUED = """{"id":"r-1","kkmId":5000021,"status":"REGISTERED",""" +
            """"registrationNumber":"000000200042","token":3735928559}"""

        /** Где на карточке стоят поле пина и «Завести кассу». */
        val PIN_FIELD = Offset(450f, 193f)
        val CONNECT = Offset(105f, 253f)
    }
}
