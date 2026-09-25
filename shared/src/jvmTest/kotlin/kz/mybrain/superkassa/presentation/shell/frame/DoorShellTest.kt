package kz.mybrain.superkassa.presentation.shell.frame

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Окно до входа — те же разделы и та же навигация, что у рабочего окна.
 *
 * Прежде вход был страницей с кнопками-дверями, и за каждой дверью —
 * своя страница: серая полоса у настроек, шапка кабинета, стрелка внутри
 * мастера. Теперь «Кассы», «Новая касса», «Кабинет БФД» и «Настройки» —
 * разделы одной навигации окна под одной шапкой: раздел называет себя
 * в шапке, стрелки назад у разделов нет — это верхний уровень, — и из
 * любого раздела навигация возвращает к кассам.
 *
 * Смена шапки в том же кадре, что и содержимого, однажды роняла сцену
 * внутри Compose, поэтому путь проходится целиком: каждый раздел — туда
 * и обратно, в светлой и тёмной теме. Кадры — `/tmp/door-<тема>-<раздел>.png`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DoorShellTest {

    private val texts = textsOf(Language.Ru).common

    /** Переходы Navigation 3 меняют жизненный цикл записей — только на главном потоке. */
    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    /** Окно без касс: все разделы на месте — мастер, кабинет и настройки. */
    private fun app(): AppContainer {
        val plain = CoreScene.app(LoginScene.core(emptyList()))
        val cabinet = CabinetRig(services = plain.services).ports
        return AppContainer(plain.services, plain.areas.copy(setup = SetupPorts(MemorySetup()), cabinet = cabinet))
    }

    @Test
    fun `каждый раздел до входа открывается навигацией окна под той же шапкой`() {
        val sections = listOf(texts.sections.register, texts.sections.cabinet, texts.sections.settings)
        listOf(Appearance.Light, Appearance.Dark).forEach { appearance ->
            RenderProbe(WIDTH, HEIGHT, appearance) { LoginScene.Door(app()) }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                File("/tmp/door-${appearance.code}-login.png").writeBytes(probe.frame())
                assertTrue(probe.nodes().any { it.text == texts.login.title }, "на кассах шапка не называет вход")
                sections.forEachIndexed { at, section ->
                    probe.tap { it.text == section }
                    repeat(SETTLE) { probe.frame() }
                    File("/tmp/door-${appearance.code}-$at.png").writeBytes(probe.frame())
                    val named = section == texts.sections.cabinet || probe.nodes().count { it.text == section } > 1
                    assertTrue(named, "шапка не называет раздел «$section»")
                    val back = probe.nodes().any { it.label == texts.settingsScreen.back }
                    assertTrue(!back, "у раздела «$section» верхнего уровня стрелка назад")
                    probe.tap { it.text == texts.sections.kkms }
                    repeat(SETTLE) { probe.frame() }
                    val home = probe.nodes().any { it.text == texts.login.title }
                    assertTrue(home, "навигация из «$section» не вернула к кассам")
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
