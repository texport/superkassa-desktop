package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Последний шаг мастера — «касса в узле» — не выдаёт чужое за своё.
 *
 * Шаг считал себя пройденным, сверяя идентификатор ОФД из черновика
 * со списком касс узла. У нетронутого мастера идентификатора нет, а у
 * кассы, заведённой вручную, сведений об ОФД в списке может не быть —
 * и пустое совпадало с пустым. Владелец открывал подключение и видел
 * последний шаг с галочкой и словами «Касса подключена — можно входить»,
 * не сделав ни одного шага.
 */
class SetupNodeStepTest {

    @Test
    fun `чужая касса без сведений об ОФД не помечает последний шаг пройденным`() {
        val alone = shot("setup-alone") { ConnectKkmScreen(workplace(), mockCabinet(NO_PLACES)) {} }
        val withKkm = shot("setup-with-kkm") {
            ConnectKkmScreen(workplace(UNKNOWN_OFD), mockCabinet(NO_PLACES)) {}
        }

        assertTrue(
            withKkm.contentEquals(alone),
            "нетронутый мастер помечает последний шаг пройденным из-за чужой кассы"
        )
    }

    private fun shot(name: String, content: @Composable () -> Unit): ByteArray =
        RenderProbe(width = WIDE, height = TALL, content = content).use { probe ->
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            File("/tmp/audit-users-$name.png").writeBytes(frame)
            frame
        }

    /** Рабочее место без узла: мастер спрашивает его только по нажатию. */
    private fun workplace(vararg known: Kkm): Session {
        val http = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        val home = Files.createTempDirectory("setup-node-step").toFile()
        return Session(ServerClient(http = http), Preferences(File(home, "kkm")))
            .also { it.kkms.addAll(known) }
    }

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40
        const val NO_PLACES = """{"page":0,"size":50,"totalElements":0,"items":[]}"""

        /** Касса на узле, про ОФД которой список ничего не говорит. */
        val UNKNOWN_OFD = Kkm(kkmId = "kkm-1", name = "Касса у входа", kkmKgdId = "000000200042")
    }
}
