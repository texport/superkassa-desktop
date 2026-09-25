package kz.mybrain.superkassa.presentation.shell.section

import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemorySetup
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.navigation.section.DashboardKey
import kz.mybrain.superkassa.navigation.section.HistoryKey
import kz.mybrain.superkassa.navigation.section.SettingsKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * История «назад» окна по разделам и разделы, собранные на платформе.
 *
 * Разделы — верхний уровень навигации: по Material 3 раздел, открытый
 * из навигации окна, встаёт над главным экраном, «назад» из любого
 * раздела ведёт на главный, а с главного — из приложения.
 */
class SectionStackTest {
    private val areas = CoreScene.app(FakeCore()).areas

    private fun history(vararg keys: NavKey): MutableList<NavKey> = mutableListOf<NavKey>(DashboardKey, *keys)

    @Test
    fun `раздел встаёт над главным экраном, а не над прошлым разделом`() {
        val history = history()
        history.openSection(Section.Sale)
        history.openSection(Section.History)

        assertEquals(listOf<NavKey>(DashboardKey, HistoryKey), history)
        assertEquals(Section.History, history.currentSection())
    }

    @Test
    fun `назад из раздела ведёт на главный, а с главного — наружу`() {
        val history = history(HistoryKey)

        assertTrue(history.stepBack())
        assertEquals(Section.Dashboard, history.currentSection())
        assertFalse(history.stepBack(), "с главного экрана «назад» уходит системе")
    }

    @Test
    fun `главный экран снимает всё над собой`() {
        val history = history(SettingsKey)
        history.openSection(Section.Dashboard)

        assertEquals(listOf<NavKey>(DashboardKey), history)
    }

    @Test
    fun `после смены кассира раздела администратора в истории быть не должно`() {
        assertTrue(history(SettingsKey).outside(sectionsFor(false, areas)))
        assertFalse(history(HistoryKey).outside(sectionsFor(false, areas)))
    }

    @Test
    fun `без кабинета и мастера их разделов не видно`() {
        val shown = sectionsFor(isAdmin = true, areas = areas)

        assertFalse(Section.Cabinet in shown)
        assertFalse(Section.Register in shown)
        assertTrue(Section.Settings in shown)
    }

    @Test
    fun `мастер без кабинета остаётся — ручным путём`() {
        val shown = sectionsFor(isAdmin = true, areas = areas.copy(setup = SetupPorts(MemorySetup())))

        assertTrue(Section.Register in shown)
        assertFalse(Section.Cabinet in shown)
    }
}
