package kz.mybrain.superkassa.presentation.setup

import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Последний шаг мастера называет причину, по которой не заводит кассу.
 *
 * Пин администратора здесь тот же, что у кассира: касса откажет пину не той
 * длины. Под полем стояла пустота, и «Завести кассу» просто не нажималась —
 * владелец видел мёртвую кнопку и ни слова о том, чем не угодил пин. Во всех
 * остальных полях пина приложения причина написана под полем.
 *
 * Пинов по умолчанию нет, и пин из одинаковых цифр — обычный пин.
 */
class SetupAdminPinTest {
    private val texts = textsOf(Language.Ru).common
    private val cashiers = textsOf(Language.Ru).kassa.money.cashiers

    @Test
    fun `короткий пин объяснён под полем`() {
        val short = card("short", "482")
        val accepted = card("accepted", "1111")

        assertTrue(cashiers.pinTooShort in short, "под полем не сказано, чем кассе не угодил пин 482")
        assertTrue(cashiers.pinTooShort !in accepted, "годный пин 1111 назван негодным")
    }

    /** Надписи карточки шага с набранным пином; поле пина ищется по подписи. */
    private fun card(name: String, pin: String): List<String> = inlineMain {
        val scene = SetupScene().started(halfway = true)
        val model = scene.model()
        RenderProbe(width = WIDE, height = TALL) { AdminStepAlone(model, scene) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.tap { it.text == texts.settingsScreen.adminPin }
            probe.type(pin)
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-admin-pin-$name.png").writeBytes(probe.frame())
            probe.nodes().map { it.text }
        }
    }

    private companion object {
        const val WIDE = 900
        const val TALL = 460
        const val SETTLE = 30
    }
}
