package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * «Начать заново» в мастере подключения спрашивает, а не стирает молча.
 *
 * Пройденное лежит на диске, и нажатие стирало его сразу: заводской номер,
 * уже унесённый в кабинет, и кассу, заведённую там под ним. Сами они
 * из кабинета не исчезают — владелец заводил вторую кассу под вторым
 * номером и разбирался с этим потом.
 *
 * Пока мастер ничего не прошёл, стирать нечего, и кнопки нет вовсе.
 * Кнопка ищется по надписи, а не по месту на экране.
 */
class SetupStartOverTest {

    @Test
    fun `нажатие только спрашивает, а пройденное остаётся на месте`(): Unit = inlineMain {
        val scene = SetupScene().started()
        val model = scene.model()
        val cabinet = CabinetStepsRig.signedIn(SetupScene.NO_PLACES)
        val models = SetupModels(model, scene.registration())
        val screen = @Composable { Windowed { ConnectKkmScreen(models, cabinet) {} } }
        RenderProbe(width = WIDE, height = TALL, content = screen).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.tap { it.text == scene.texts.startOver }
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-start-over-ask.png").writeBytes(probe.frame())

            assertTrue(probe.nodes().any { it.text == scene.texts.startOverAsk }, "вопрос о начале заново не появился")
            val kept = model.state.value.draft.factoryNumber
            assertEquals(SetupScene.FACTORY, kept, "пройденное стёрлось без ответа владельца")
            assertEquals(SetupScene.FACTORY, scene.memory.setupValue("factory"), "пройденное стёрто на диске")
        }
    }

    @Test
    fun `нетронутый мастер стирать нечем`(): Unit = inlineMain {
        val scene = SetupScene()
        val model = scene.model()
        val cabinet = CabinetStepsRig.signedIn(SetupScene.NO_PLACES)
        val models = SetupModels(model, scene.registration())
        val screen = @Composable { Windowed { ConnectKkmScreen(models, cabinet) {} } }
        RenderProbe(width = WIDE, height = TALL, content = screen).use { probe ->
            repeat(SETTLE) { probe.frame() }
            val nodes = probe.nodes()
            assertTrue(nodes.any { it.text == scene.texts.title }, "мастер не нарисован: $nodes")
            assertTrue(nodes.none { it.text == scene.texts.startOver }, "у нетронутого мастера есть «Начать заново»")
            assertNull(model.state.value.draft.factoryNumber)
        }
    }

    private companion object {
        const val WIDE = 1372
        const val TALL = 887
        const val SETTLE = 20
    }
}
