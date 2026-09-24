package kz.mybrain.superkassa.presentation.users

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Смена пина чужому кассиру не переселяет работу на его пин.
 *
 * Пин меняют и себе, и другим: сменивший себе продолжает работу новым
 * пином, а не упирается в отказ на удавшейся операции. Но новый пин
 * принадлежит тому кассиру, которому его задали. Подстановка чужого пина
 * подписывала бы его именем чеки администратора.
 *
 * Кнопки ищутся по надписи в строке кассира, а не по месту на экране.
 */
class UsersOwnPinTest {
    private val texts = textsOf(Language.Ru).common

    @Test
    fun `чужой пин не становится пином работающего`(): Unit = inlineMain {
        val scene = UsersScene(pin = OWN_PIN)
        val model = scene.model()
        RenderProbe(width = WIDE, height = TALL) { UsersScreen(model) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val button = probe.inRowOf(UsersScene.CASHIER.name) { it.text == texts.users.newPin }
            probe.click(button.at + Offset(button.width / 2f, button.height / 2f))
            repeat(SETTLE) { probe.frame() }
            probe.type(NEW_PIN)
            File("/tmp/audit-users-pin-dialog.png").writeBytes(probe.frame())
            probe.tap { it.text == texts.users.change }
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-pin-changed.png").writeBytes(probe.frame())

            assertTrue(scene.asked.any { it.startsWith("updateUser") }, "пин кассиру так и не сменили: ${scene.asked}")
            assertEquals(OWN_PIN, scene.signIn.state.value.pin, "работа продолжилась под пином чужого кассира")
            assertFalse(scene.asked.any { it.endsWith(" $NEW_PIN") }, "касса спрошена чужим пином: ${scene.asked}")
        }
    }

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40

        /** Пин, под которым работает администратор, и пин, который он задаёт кассиру. */
        const val OWN_PIN = "1234"
        const val NEW_PIN = "5555"
    }
}
