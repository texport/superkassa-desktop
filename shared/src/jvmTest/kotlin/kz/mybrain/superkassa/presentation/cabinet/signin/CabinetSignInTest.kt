package kz.mybrain.superkassa.presentation.cabinet.signin

import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Дверь в кабинет одна — по ЭЦП.
 *
 * Вход по набранным ИИН и БИН стоял на том же экране и предлагал ввести
 * любые двенадцать цифр: на экране входа это вторая, неохраняемая дверь.
 * Сперва его спрятали за режим отладки, затем убрали целиком вместе
 * с подпоркой в сеансе — личность владельца приходит только
 * из сертификата.
 *
 * Проверяется кадром сцены: включённая отладка не должна менять экран
 * входа ни на один пиксель, иначе дверь где-то осталась.
 */
class CabinetSignInTest {

    private val was = AppLog.debugMode

    @AfterTest
    fun restore() {
        AppLog.switchDebugMode(was)
    }

    private fun door(debug: Boolean): ByteArray {
        AppLog.switchDebugMode(debug)
        val state = CabinetUiState(address = CabinetSettings.DEFAULT_URL)
        val texts = textsOf(Language.Ru).cabinet
        return RenderProbe { CabinetSignIn(state, Language.Ru, texts, object : CabinetActions {}) }
            .use { probe ->
                val frame = probe.frame()
                File("/tmp/signin-${if (debug) "debug" else "plain"}.png").writeBytes(frame)
                frame
            }
    }

    @Test
    fun `на экране входа только ЭЦП при любом режиме`() {
        val plain = door(debug = false)
        val debug = door(debug = true)

        assertTrue(plain.isNotEmpty() && debug.isNotEmpty())
        assertTrue(plain.contentEquals(debug), "режим отладки всё ещё открывает вход по ИИН и БИН")
    }
}
