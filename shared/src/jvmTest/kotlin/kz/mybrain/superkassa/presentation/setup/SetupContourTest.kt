package kz.mybrain.superkassa.presentation.setup

import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Касса не заводится в контур, которого владелец не выбирал.
 *
 * Контуры БФД называет касса. Пока она их не назвала, выбирать не из чего,
 * и подставлять в поле нечего — а кнопка всё равно оживала от одного
 * годного пина и отправляла заведение с пустым контуром. Касса отвечала
 * на это отказом, но токен кабинета к тому мигу уже был выдан на эту кассу.
 * Ручной путь подключения так не делает: там заведение ждёт выбранного
 * контура.
 */
class SetupContourTest {
    private val texts = stringsOf(Language.Ru)

    @Test
    fun `без выбранного контура касса не заводится`(): Unit = inlineMain {
        val scene = SetupScene(contours = null).started(halfway = true)
        scene.cabinet.token = ISSUED_TOKEN
        val model = scene.model()
        RenderProbe(width = WIDE, height = TALL) { AdminStepAlone(model, scene) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.tap { it.text == texts.settings.adminPin }
            probe.type(GOOD_PIN)
            repeat(SETTLE) { probe.frame() }
            probe.tap { it.text == scene.texts.connect }
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-contour-empty.png").writeBytes(probe.frame())

            assertTrue(model.state.value.form.adminPin == GOOD_PIN, "пин не набран: проверка ничего не проверила")
            val calls = scene.core.calls
            assertTrue("initKkmSimple" !in calls, "касса заведена без выбранного контура: $calls")
        }
    }

    private companion object {
        const val WIDE = 900
        const val TALL = 460
        const val SETTLE = 30
        const val GOOD_PIN = "4821"

        /** Кабинет токен выдаёт: касса не заводится только из-за контура. */
        const val ISSUED_TOKEN = "3735928559"
    }
}
