package kz.mybrain.superkassa.desktop

import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.theme.Accent
import kz.mybrain.superkassa.desktop.ui.theme.DarkScheme
import kz.mybrain.superkassa.desktop.ui.theme.LightScheme
import kz.mybrain.superkassa.desktop.ui.theme.schemeOf
import kz.mybrain.superkassa.desktop.ui.theme.tonalScheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Тон кассы меняет главную роль и не меняет ничего сверх задуманного.
 *
 * Схемы выводятся из оттенка правилами тональных палитр, и ошибка
 * в правиле не падает сборкой: она выходит одинаковыми кружками
 * в выборе тона или синим отказом на красной кассе.
 */
class LookPaletteTest {

    @Test
    fun `у каждого тона своя главная роль в обеих темах`() {
        listOf(false, true).forEach { dark ->
            val primaries = Accent.entries.map { schemeOf(it, dark).primary }
            assertEquals(primaries.size, primaries.toSet().size, "тона совпали главной ролью, dark=$dark")
        }
    }

    /**
     * Кружки в выборе тона различимы глазом, а не только числами.
     *
     * Равенства главных ролей мало: два тона могут разойтись на единицу
     * из двухсот пятидесяти пяти и остаться для кассира одним и тем же
     * кружком. Порог — вчетверо выше порога различения глазом; самая
     * тесная пара круга, изумрудный с бирюзовым, держится выше него
     * с запасом, а пятнадцатый тон пришлось бы ставить уже под ним.
     */
    @Test
    fun `ни один тон не сливается с соседним`() {
        listOf(false, true).forEach { dark ->
            val primaries = Accent.entries.map { it to schemeOf(it, dark).primary }
            primaries.forEachIndexed { at, (accent, color) ->
                primaries.drop(at + 1).forEach { (other, second) ->
                    val gap = LookColors.distance(color, second)
                    assertTrue(gap >= APART, "$accent и $other сошлись при dark=$dark, ΔE $gap")
                }
            }
        }
    }

    @Test
    fun `индиго без выбора остаётся тем, чем касса была`() {
        assertSame(LightScheme, schemeOf(Accent.Indigo, dark = false))
        assertSame(DarkScheme, schemeOf(Accent.Indigo, dark = true))
        assertEquals(Accent.Indigo, Accent.byCode(null))
        assertEquals(Accent.Indigo, Accent.byCode("no-such-accent"))
    }

    /**
     * Правила вывода откалиброваны по индиго: выведенный из его оттенка
     * индиго обязан почти совпасть с выписанным руками. Разошёлся —
     * значит, правила ушли и остальные тона выйдут не по Material.
     */
    @Test
    fun `выведенный индиго совпадает с эталонным`() {
        val derived = tonalScheme(Accent.Indigo.hue, dark = false)
        assertClose(LightScheme.primary, derived.primary, "primary")
        assertClose(LightScheme.secondary, derived.secondary, "secondary")
        assertClose(LightScheme.tertiary, derived.tertiary, "tertiary")
        assertClose(LightScheme.surface, derived.surface, "surface")
        assertClose(LightScheme.surfaceContainerHighest, derived.surfaceContainerHighest, "surfaceContainerHighest")
        assertClose(LightScheme.outline, derived.outline, "outline")
        val dark = tonalScheme(Accent.Indigo.hue, dark = true)
        assertClose(DarkScheme.surface, dark.surface, "dark surface")
        assertClose(DarkScheme.onSurface, dark.onSurface, "dark onSurface")
    }

    @Test
    fun `отказ красный при любом тоне`() {
        Accent.entries.forEach { accent ->
            assertEquals(LightScheme.error, schemeOf(accent, dark = false).error, "$accent")
            assertEquals(DarkScheme.error, schemeOf(accent, dark = true).error, "$accent")
        }
    }

    @Test
    fun `светлая и тёмная схемы одного тона различаются поверхностью`() {
        Accent.entries.forEach { accent ->
            val light = schemeOf(accent, dark = false)
            val dark = schemeOf(accent, dark = true)
            assertNotEquals(light.surface, dark.surface, "$accent")
            assertTrue(light.surface.red > dark.surface.red, "$accent: светлая поверхность темнее тёмной")
        }
    }

    private fun assertClose(expected: Color, actual: Color, role: String) {
        val distance = listOf(
            expected.red - actual.red,
            expected.green - actual.green,
            expected.blue - actual.blue
        ).maxOf { abs(it) }
        assertTrue(distance <= TOLERANCE, "$role: ожидалось $expected, вышло $actual")
    }

    private companion object {
        /** Две ступени из 255 на канал: глазом не отличить. */
        const val TOLERANCE = 2.5f / 255f

        /** Насколько расходятся главные роли двух соседних тонов. */
        const val APART = 10f
    }
}
