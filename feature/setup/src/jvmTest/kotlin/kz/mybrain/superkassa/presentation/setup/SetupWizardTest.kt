package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import io.github.texport.superkassa.core.presentation.api.model.common.FactoryNumberResponse
import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Мастер новой кассы идёт по шагу на экран.
 *
 * Владелец: «очень много информации, мастер должен быть разбит на шаги».
 * На экране — один шаг с ходом «Шаг 2 из 5», «Далее» ведёт вперёд, «Назад»
 * и история окна — назад; брошенный мастер открывается там, где его
 * оставили. Кнопки и шаги ищутся по надписям.
 */
class SetupWizardTest {

    @Test
    fun `на экране один шаг, и «Далее» ведёт на следующий`(): Unit = inlineMain {
        val scene = SetupScene()
        val history = WizardHistory()
        RenderProbe(width = WIDE, height = TALL) { WizardOf(scene, history, CabinetStepsRig.idle()) }.use { probe ->
            settle(probe, "first")
            val first = probe.nodes().map { it.text }
            assertTrue(scene.texts.stepWay in first, "первый шаг не выбор пути: $first")
            assertTrue(scene.texts.stepOf.fill(1, 5) in first, "хода мастера нет: $first")
            assertFalse(scene.texts.stepFactory in first, "на первом шаге видно следующий")

            probe.tap { it.text == scene.texts.next }
            settle(probe, "factory")
            assertEquals(listOf(SetupStep.Factory.name), history.steps)
            assertTrue(probe.nodes().any { it.text == scene.texts.stepOf.fill(2, 5) }, "ход мастера не сдвинулся")
            assertFalse(probe.pressable(scene.texts.next), "без номера мастер пускает дальше")
        }
    }

    @Test
    fun `полученный номер пускает дальше, «Назад» возвращает`(): Unit = inlineMain {
        val scene = SetupScene()
        scene.core.on("generateFactoryInfo") { FACTORY }
        val history = WizardHistory(SetupStep.Factory.name)
        RenderProbe(width = WIDE, height = TALL) { WizardOf(scene, history, CabinetStepsRig.idle()) }.use { probe ->
            settle(probe, "factory-empty")
            probe.tap { it.text == scene.texts.getFactory }
            settle(probe, "factory-got")
            assertTrue(probe.nodes().any { it.text == FACTORY.factoryNumber }, "полученный номер не показан")
            assertTrue(probe.pressable(scene.texts.next), "с номером мастер не пускает дальше")

            probe.tap { it.text == scene.texts.back }
            settle(probe, "back")
            assertEquals(emptyList(), history.steps, "«Назад» не вернул на первый шаг")
        }
    }

    @Test
    fun `брошенный мастер открывается на шаге, где его оставили`(): Unit = inlineMain {
        val scene = SetupScene().started(halfway = true)
        scene.memory.setupValue("way", "ViaCabinet")
        val history = WizardHistory()
        RenderProbe(width = WIDE, height = TALL) { WizardOf(scene, history, CabinetStepsRig.idle()) }.use { probe ->
            settle(probe, "resumed")
            val cabinet = listOf(SetupStep.Factory, SetupStep.Cabinet, SetupStep.Application).map { it.name }
            assertEquals(cabinet, history.steps, "мастер открылся не на шаге учёта")
            assertTrue(probe.nodes().any { it.text == scene.texts.stepApplication })

            probe.tap { it.text == scene.texts.back }
            settle(probe, "resumed-back")
            val shown = probe.nodes().map { it.text }
            assertTrue(scene.texts.addedToCabinet in shown, "пройденный шаг не показан сделанным: $shown")
        }
    }

    @Test
    fun `шаг, на котором стоять нельзя, снимается сам`(): Unit = inlineMain {
        val scene = SetupScene()
        val history = WizardHistory(SetupStep.Factory.name, SetupStep.Cabinet.name, SetupStep.Application.name)
        RenderProbe(width = WIDE, height = TALL) { WizardOf(scene, history, CabinetStepsRig.idle()) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            assertEquals(listOf(SetupStep.Factory.name), history.steps, "шаги без пройденного остались в истории")
        }
    }

    @Test
    fun `ручной путь без кабинета — данные БФД, пин, и мастер снова в начале`(): Unit = inlineMain {
        val scene = SetupScene()
        scene.core.on("initKkmSimple") { CoreScene.kkm(id = "kkm-9") }
        scene.core.on("getKkm") { CoreScene.kkm(id = "kkm-9") }
        val model = setupModel(CoreScene.services(scene.core), SetupPorts(scene.memory), WithoutCabinet)
        val history = WizardHistory()
        val screen = @Composable { Windowed { WizardShown(history) { step -> ConnectByHand(model, step) } } }
        RenderProbe(width = WIDE, height = TALL, content = screen).use { probe ->
            settle(probe, "manual-factory")
            assertTrue(probe.nodes().any { it.text == scene.texts.stepOf.fill(1, 3) }, "без кабинета шагов не три")
            probe.tap { it.text == scene.texts.next }
            settle(probe, "manual-credentials")
            probe.fill(fields.kkmIdentifier, "5000021")
            probe.fill(fields.token, "3735928559")
            probe.tap { it.text == scene.texts.next }
            settle(probe, "manual-admin")
            probe.fill(fields.adminPin, PIN)
            probe.fill(scene.texts.pinRepeat, PIN)
            probe.tap { it.text == scene.texts.connect }
            settle(probe, "manual-done")

            assertTrue("initKkmSimple" in scene.core.calls, "касса не заведена: ${scene.core.calls}")
            assertEquals(emptyList(), history.steps, "заведённая касса оставила мастер на последнем шаге")
        }
    }

    /** Набрать [text] в поле с подписью [label]. */
    private fun RenderProbe.fill(label: String, text: String) {
        tap { it.text == label }
        type(text)
        repeat(SETTLE) { frame() }
    }

    private val fields = textsOf(Language.Ru).common.settingsScreen

    private fun settle(probe: RenderProbe, name: String) {
        repeat(SETTLE) { probe.frame() }
        File("/tmp/audit-setup-wizard-$name.png").writeBytes(probe.frame())
    }

    private companion object {
        const val WIDE = 1280
        const val TALL = 800
        const val SETTLE = 30
        const val PIN = "4821"
        val FACTORY = FactoryNumberResponse("KZT26E2C509A200", 2026)
    }
}
