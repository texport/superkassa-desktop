package kz.mybrain.superkassa.presentation.shell.section

import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemorySetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Путь по разделам для жеста «назад» и разделы, собранные на платформе. */
class SectionTrailTest {
    private val areas = CoreScene.app(FakeCore()).areas

    @Test
    fun `назад ведёт к предыдущему разделу, а с главного — наружу`() {
        val trail = SectionTrail().open(Section.Sale).open(Section.History)

        assertEquals(Section.Sale, trail.back().current)
        assertEquals(Section.Dashboard, trail.back().back().current)
        assertFalse(trail.back().back().canGoBack, "с главного экрана «назад» сворачивает приложение")
    }

    @Test
    fun `раздел, открытый второй раз, встаёт наверх, а не повторяется`() {
        val trail = SectionTrail().open(Section.Sale).open(Section.History).open(Section.Sale)

        assertEquals(listOf(Section.Dashboard, Section.History, Section.Sale), trail.sections)
    }

    @Test
    fun `главный экран начинает путь заново`() {
        val trail = SectionTrail().open(Section.Sale).open(Section.Dashboard)

        assertEquals(SectionTrail(), trail)
    }

    @Test
    fun `после смены кассира из пути уходят разделы администратора`() {
        val trail = SectionTrail().open(Section.Sale).open(Section.Settings)

        assertEquals(listOf(Section.Dashboard, Section.Sale), trail.within(sectionsFor(false, areas)).sections)
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
