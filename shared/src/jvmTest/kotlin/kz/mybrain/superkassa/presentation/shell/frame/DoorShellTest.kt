package kz.mybrain.superkassa.presentation.shell.frame

import kz.mybrain.superkassa.CabinetRig
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.MemorySetup
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Шапка окна до входа — одна на вход и на всё, что за его дверями.
 *
 * Прежде у каждой двери была своя шапка: серая полоса у настроек,
 * шапка кабинета, стрелка внутри мастера у заведения кассы — и шапка
 * прыгала от двери к двери. Теперь её ставит каркас: за дверью в шапке
 * название двери и стрелка назад, и стрелка возвращает на вход.
 *
 * Смена шапки в том же кадре, что и содержимого, однажды роняла сцену
 * внутри Compose, поэтому путь проходится целиком: каждая дверь —
 * туда и обратно, в светлой и тёмной теме. Кадры —
 * `/tmp/door-<тема>-<дверь>.png`.
 */
class DoorShellTest {

    private val texts = textsOf(Language.Ru).common

    /** Окно без касс: все три двери на месте — мастер, кабинет и настройки. */
    private fun app(): AppContainer {
        val plain = CoreScene.app(LoginScene.core(emptyList()))
        val cabinet = CabinetRig(services = plain.services).ports
        return AppContainer(plain.services, plain.areas.copy(setup = SetupPorts(MemorySetup()), cabinet = cabinet))
    }

    @Test
    fun `каждая дверь открывается под шапкой окна и закрывается её стрелкой`() {
        val doors = listOf(texts.sections.register, texts.sections.cabinet, texts.sections.settings)
        listOf(Appearance.Light, Appearance.Dark).forEach { appearance ->
            RenderProbe(WIDTH, HEIGHT, appearance) { LoginScene.Door(app()) }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                File("/tmp/door-${appearance.code}-login.png").writeBytes(probe.frame())
                assertTrue(probe.nodes().any { it.text == texts.login.title }, "на входе шапка не называет вход")
                doors.forEachIndexed { at, door ->
                    probe.tap { it.text == door }
                    repeat(SETTLE) { probe.frame() }
                    File("/tmp/door-${appearance.code}-$at.png").writeBytes(probe.frame())
                    val back = probe.nodes().any { it.label == texts.settingsScreen.back }
                    assertTrue(back, "за дверью «$door» нет стрелки назад")
                    probe.tap { it.label == texts.settingsScreen.back }
                    repeat(SETTLE) { probe.frame() }
                    val home = probe.nodes().any { it.text == texts.login.title }
                    assertTrue(home, "стрелка из «$door» не вернула на вход")
                }
            }
        }
    }

    private companion object {
        const val WIDTH = 1280
        const val HEIGHT = 800
        const val SETTLE = 40
    }
}
