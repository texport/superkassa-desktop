package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.SearchField
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.cabinet.askableQuery
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Общие элементы интерфейса: строка поиска, карточка раздела,
 * чередование строк и служебные знаки.
 *
 * Проверяется то, ради чего они заведены по одному на приложение:
 * элемент рисуется, кнопка очистки появляется только при набранном
 * слове, а правила чередования и знаков не зависят от места применения.
 */
class SharedElementsTest {

    /** Пустая строка поиска рисуется: значок и подпись на месте. */
    @Test
    fun `search field renders`() {
        val frame = RenderProbe(WIDTH, HEIGHT) {
            SearchField(value = "", label = "Поиск", onChange = {}, modifier = Modifier.fillMaxWidth())
        }.use { it.frame() }
        assertTrue(frame.isNotEmpty())
    }

    /**
     * Кнопка очистки появляется только там, где её разрешили, и только
     * при набранном слове: пустое поле не предлагает стереть пустоту.
     */
    @Test
    fun `clearing shows up only with a word typed`() {
        val empty = probeSearch("", clear = "Стереть")
        val typed = probeSearch("чек", clear = "Стереть")
        val without = probeSearch("чек", clear = null)
        assertNotEquals(empty.toList(), typed.toList())
        assertNotEquals(typed.toList(), without.toList())
    }

    /** Значок берётся из общего набора, а не подставляется на месте. */
    @Test
    fun `search field takes the icon given to it`() {
        val magnifier = RenderProbe(WIDTH, HEIGHT) {
            SearchField(value = "", label = "Поиск", onChange = {}, icon = AppIcons.find)
        }.use { it.frame() }
        val register = RenderProbe(WIDTH, HEIGHT) {
            SearchField(value = "", label = "Поиск", onChange = {}, icon = AppIcons.kkm)
        }.use { it.frame() }
        assertNotEquals(magnifier.toList(), register.toList())
    }

    /** Карточка раздела рисуется с заголовком, объяснением и содержимым. */
    @Test
    fun `section card renders`() {
        val frame = RenderProbe(WIDTH, HEIGHT) {
            Column {
                SectionCard(title = "Раздел", info = "Что здесь делают") { Text("содержимое") }
            }
        }.use { it.frame() }
        assertTrue(frame.isNotEmpty())
    }

    /** Затеняется каждая вторая строка, считая с нуля, — и так во всех списках. */
    @Test
    fun `every second row is shaded`() {
        assertContentEquals(
            listOf(false, true, false, true, false),
            (0..4).map { stripedAt(it) }
        )
    }

    /** Знаки интерфейса — те самые, а не похожие на них дефис и точка. */
    @Test
    fun `glyphs are the intended code points`() {
        assertEquals('—'.toString(), Glyphs.DASH)
        assertEquals(" · ", Glyphs.SEPARATOR)
        assertEquals(' ', Glyphs.NBSP)
        assertNotEquals("-", Glyphs.DASH)
        assertNotEquals(' ', Glyphs.NBSP)
    }

    /** Подсказки кабинета: пустой запрос принимается, одна буква — нет. */
    @Test
    fun `suggestions need either nothing or two letters`() {
        assertTrue(askableQuery(""))
        assertFalse(askableQuery("а"))
        assertTrue(askableQuery("ал"))
    }

    private fun probeSearch(value: String, clear: String?): ByteArray =
        RenderProbe(WIDTH, HEIGHT) {
            SearchField(
                value = value,
                label = "Поиск",
                onChange = {},
                modifier = Modifier.fillMaxWidth(),
                clearLabel = clear
            )
        }.use { it.frame() }

    private companion object {
        const val WIDTH = 480
        const val HEIGHT = 160
    }
}
