package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.adaptive.HeightClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.adaptive.WindowClass
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.section.BarLead
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Навигация окна по разделам — по рекомендации Material 3.
 *
 * Телефон — нижняя полоса, пока разделов не больше пяти, как у кассира;
 * у администратора их десять — тогда все разделы в модальном рельсе
 * по кнопке меню в шапке. Шире — рельс, на большом мониторе развёрнутый.
 * Кадры — `/tmp/frame-<окно>-<кто>.png`.
 */
class ShellFrameTest {

    private val cashier = Section.entries.filterNot { it.adminOnly }
    private val texts = textsOf(Language.Ru).common

    @Composable
    private fun Frame(sections: List<Section>) {
        ShellFrame(sections, Section.Dashboard, {}, topBar = { onMenu -> MenuProbe(onMenu) }) {}
    }

    @Test
    fun `вид навигации следует классу окна и числу разделов`() {
        val phone = WindowClass(WidthClass.Compact, HeightClass.Expanded)
        val landscape = WindowClass(WidthClass.Medium, HeightClass.Compact)
        val desktop = WindowClass(WidthClass.Expanded, HeightClass.Expanded)
        val monitor = WindowClass(WidthClass.ExtraLarge, HeightClass.Expanded)

        assertEquals(ShellNavigation.Bar, ShellNavigation.of(phone, cashier.size))
        assertEquals(ShellNavigation.Modal, ShellNavigation.of(phone, Section.entries.size))
        assertEquals(ShellNavigation.LowBar, ShellNavigation.of(landscape, cashier.size))
        assertEquals(ShellNavigation.Rail, ShellNavigation.of(desktop, Section.entries.size))
        // Большое окно — тот же рельс: развёрнутые разделы открываются поверх окна.
        assertEquals(ShellNavigation.Rail, ShellNavigation.of(monitor, Section.entries.size))
    }

    @Test
    fun `у кассира на телефоне все разделы в нижней полосе`() {
        RenderProbe(PHONE_W, PHONE_H) { Frame(cashier) }.use { probe ->
            File("/tmp/frame-phone-cashier.png").writeBytes(probe.frame())
            cashier.forEach { section ->
                assertTrue(probe.nodes().any { it.text == section.title(texts.sections) }, "$section нет в полосе")
            }
            assertTrue(probe.nodes().none { it.label == texts.sections.menu }, "кассиру не нужна кнопка меню")
        }
    }

    @Test
    fun `у администратора на телефоне разделы открываются кнопкой меню`() {
        RenderProbe(PHONE_W, PHONE_H) { Frame(Section.entries) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            assertTrue(probe.nodes().any { it.label == texts.sections.menu }, "нет кнопки меню в шапке")
            val menu = probe.centerOf(texts.sections.menu)
            probe.tap(texts.sections.menu)
            repeat(SETTLE) { probe.frame() }
            File("/tmp/frame-phone-admin.png").writeBytes(probe.frame())
            val collapse = textsOf(Language.Ru).common.general.collapse
            assertEquals(menu, probe.centerOf(collapse), "кнопка закрытия не на месте кнопки меню в шапке")
            val settings = texts.sections.settings
            assertTrue(probe.nodes().any { it.text == settings }, "в модальном рельсе нет настроек")
        }
    }

    @Test
    fun `на большом окне кнопка меню рельса открывает разделы поверх окна`() {
        val general = textsOf(Language.Ru).common.general
        RenderProbe(width = WIDE_W, height = WIDE_H) { Frame(Section.entries) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val title = probe.nodes().first { it.text == "Касса у входа" }.at.x
            val menu = probe.centerOf(general.expand)
            probe.tap(general.expand)
            repeat(SETTLE) { probe.frame() }
            File("/tmp/frame-wide-menu.png").writeBytes(probe.frame())
            assertTrue(probe.nodes().any { it.label == general.collapse }, "разделы поверх окна не открылись")
            assertEquals(menu, probe.centerOf(general.collapse), "кнопка закрытия не на месте кнопки меню")
            val moved = probe.nodes().first { it.text == "Касса у входа" }.at.x
            assertEquals(title, moved, "развёрнутые разделы отодвинули окно")
        }
    }

    @Test
    fun `в низком окне разделы рельса прокручиваются`() {
        RenderProbe(width = DESK_W, height = LOW_H) { Frame(Section.entries) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            File("/tmp/frame-low-admin.png").writeBytes(before)
            probe.wheel(at = Offset(RAIL_X, LOW_H / 2f), ticks = WHEEL)
            assertTrue(probe.changedFrom(before), "разделы рельса в низком окне не прокручиваются")
        }
    }

    private fun RenderProbe.tap(label: String) = click(centerOf(label))

    /** Середина кнопки с подписью [label] для чтения с экрана. */
    private fun RenderProbe.centerOf(label: String): Offset {
        val node = nodes().first { it.label == label }
        return Offset(node.at.x + node.width / 2f, node.at.y + node.height / 2f)
    }

    private companion object {
        const val PHONE_W = 411
        const val PHONE_H = 891
        const val DESK_W = 1000
        const val WIDE_W = 1920
        const val WIDE_H = 1080
        const val LOW_H = 640
        const val RAIL_X = 48f
        const val WHEEL = 6f
        const val SETTLE = 20
    }
}

/** Шапка проверки: кнопка меню, если её дали. */
@Composable
private fun MenuProbe(onMenu: (() -> Unit)?) {
    AppTopBar(
        title = "Касса у входа",
        lead = onMenu?.let { BarLead.Menu(it, textsOf(Language.Ru).common.sections.menu) }
    ) {}
}
