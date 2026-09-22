package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.setup.AdminStepCard
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Последний шаг мастера называет причину, по которой не заводит кассу.
 *
 * Пин администратора здесь тот же, что у кассира: узел откажет и по длине,
 * и по стандартному пину. Под полем стояла пустота, и «Завести кассу»
 * просто не нажималась — владелец, набравший 1111, видел мёртвую кнопку
 * и ни слова о том, чем ему не угодил пин. Во всех остальных полях пина
 * приложения причина написана под полем.
 */
class SetupAdminPinTest {

    @Test
    fun `запрещённый пин объяснён под полем`() {
        val forbidden = card("forbidden", "1111")
        val accepted = card("accepted", "4821")

        assertTrue(
            bottomOfCard(forbidden) > bottomOfCard(accepted),
            "под полем пина не сказано, чем узлу не угодил пин 1111"
        )
    }

    /** Карточка шага с набранным пином. */
    private fun card(name: String, pin: String): ByteArray {
        val session = workplace()
        val draft = KkmSetupDraft(session.preferences).apply {
            rememberFactory("KZT26E2C509A200", "2026")
            rememberRegister("r-1", 5_000_021, "Касса у входа")
        }
        return RenderProbe(width = WIDE, height = TALL) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                AdminStepCard(session, mockCabinet(ON_RECORD), setupTexts(Language.Ru), draft, true) {}
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(PIN_FIELD)
            probe.type(pin)
            val frame = probe.frame()
            File("/tmp/audit-users-admin-pin-$name.png").writeBytes(frame)
            frame
        }
    }

    /**
     * Нижний край нарисованного: строка кадра, ниже которой одна подложка.
     *
     * Карточка растёт ровно на ту строку, которой объяснён отказ, и сдвиг
     * нижнего края — это и есть появление объяснения. Сравнение кадров
     * целиком тут не годится: кадры различаются и без объяснения — одним
     * набранным пином.
     */
    private fun bottomOfCard(png: ByteArray): Int {
        val image: BufferedImage = ImageIO.read(ByteArrayInputStream(png))
        val backdrop = image.getRGB(image.width - 1, image.height - 1)
        for (row in image.height - 1 downTo 0) {
            val line = image.getRGB(0, row, image.width, 1, null, 0, image.width)
            if (line.any { it != backdrop }) return row
        }
        return 0
    }

    private fun workplace(): Session {
        val http = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        val home = Files.createTempDirectory("setup-admin-pin").toFile()
        return Session(ServerClient(http = http), Preferences(File(home, "kkm")))
    }

    private companion object {
        const val WIDE = 900
        const val TALL = 460
        const val SETTLE = 30
        const val ON_RECORD = """{"id":"r-1","kkmId":5000021,"status":"REGISTERED",""" +
            """"registrationNumber":"000000200042"}"""

        /** Где на карточке стоит поле пина администратора. */
        val PIN_FIELD = Offset(450f, 193f)
    }
}
