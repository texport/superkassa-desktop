package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Вход по ЭЦП предлагается один раз, а не двумя одинаковыми кнопками.
 *
 * Вход нужен всем шагам, кроме первого, поэтому кнопка стоит и над
 * шагами — для того, кто вернулся к мастеру на середине и у кого своей
 * кнопки не осталось ни в одном открытом шаге. Но шаг «Касса в кабинете»
 * предлагает вход и сам, и владелец, дошедший до него, видел две
 * одинаковые синие кнопки подряд: одну без единого слова над карточками,
 * вторую под объяснением, зачем она.
 */
class SetupSignInTest {

    @Test
    fun `над шагами не стоит второй такой же вход`() {
        val shut = shot("sign-in-shut", CabinetSession())
        val open = shot("sign-in-open", mockCabinet(NO_PLACES))

        assertEquals(
            firstCardTop(open),
            firstCardTop(shut),
            "над шагами мастера стоит вторая такая же кнопка входа"
        )
        assertFalse(shut.contentEquals(open), "закрытый и открытый кабинет на экране неразличимы")
    }

    /**
     * Продолженный назавтра мастер входом не обделён.
     *
     * Все открытые шаги такого мастера ждут кабинета и своей кнопки входа
     * не рисуют: она живёт в шаге «Касса в кабинете», а он уже пройден.
     * Без кнопки над шагами владелец упирался бы в «ждёт предыдущего шага»
     * на всех трёх оставшихся.
     */
    @Test
    fun `на середине мастера вход предлагается над шагами`() {
        val shut = shot("sign-in-halfway-shut", CabinetSession(), halfway = true)
        val open = shot("sign-in-halfway-open", mockCabinet(NO_PLACES), halfway = true)

        assertTrue(
            firstCardTop(shut) > firstCardTop(open),
            "мастеру, продолженному назавтра, войти в кабинет нечем"
        )
    }

    private fun shot(name: String, cabinet: CabinetSession, halfway: Boolean = false): ByteArray {
        val session = workplace()
        KkmSetupDraft(session.preferences).apply {
            rememberFactory(FACTORY, YEAR)
            if (halfway) rememberRegister("r-1", 5_000_021, "Касса у входа")
        }
        return RenderProbe(width = WIDE, height = TALL) { ConnectKkmScreen(session, cabinet) {} }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            File("/tmp/audit-users-$name.png").writeBytes(frame)
            frame
        }
    }

    /**
     * Строка, с которой начинается карточка первого шага.
     *
     * Меряется по середине кадра: ряд сегментов и кнопки жмутся к левому
     * краю, а карточки шагов идут во всю ширину, и первое нарисованное
     * посередине под шапкой — это верхняя граница первой карточки.
     */
    private fun firstCardTop(png: ByteArray): Int {
        val image = ImageIO.read(ByteArrayInputStream(png))
        val backdrop = image.getRGB(image.width / 2, BELOW_HEADER)
        for (row in BELOW_HEADER until image.height) {
            if (image.getRGB(image.width / 2, row) != backdrop) return row
        }
        return image.height
    }

    private fun workplace(): Session {
        val http = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        val home = Files.createTempDirectory("setup-sign-in").toFile()
        return Session(ServerClient(http = http), Preferences(File(home, "kkm")))
    }

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40
        const val FACTORY = "KZT26E2C509A200"
        const val YEAR = "2026"
        const val NO_PLACES = """{"page":0,"size":50,"totalElements":0,"items":[]}"""

        /** Первая строка под шапкой и рядом сегментов: ниже неё идут шаги. */
        const val BELOW_HEADER = 125
    }
}
