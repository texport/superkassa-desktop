package kz.mybrain.superkassa.desktop

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kz.mybrain.superkassa.desktop.ui.theme.AppTypography
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import kz.mybrain.superkassa.desktop.ui.theme.Typeface
import kz.mybrain.superkassa.desktop.ui.theme.typographyOf
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Размер и шрифт ложатся на всю шкалу, а не на часть ролей.
 *
 * Роль, забытая при сборке шкалы, останется прежнего кегля и прежнего
 * семейства, и на крупной кассе одна подпись будет мелкой — заметить
 * такое глазами на экране настроек нельзя, роли там не все.
 */
class LookTypographyTest {

    @Test
    fun `множитель масштабирует каждую роль шкалы`() {
        TextScale.entries.forEach { scale ->
            val scaled = typographyOf(Typeface.System, scale)
            roles(AppTypography).zip(roles(scaled)).forEach { (base, actual) ->
                assertNear(base.fontSize.value * scale.factor, actual.fontSize.value, "кегль $base при $scale")
                assertNear(base.lineHeight.value * scale.factor, actual.lineHeight.value, "строка $base при $scale")
                assertEquals(base.fontWeight, actual.fontWeight, "начертание при $scale")
            }
        }
    }

    @Test
    fun `обычный размер оставляет шкалу как есть`() {
        roles(AppTypography).zip(roles(typographyOf(Typeface.System, TextScale.Normal))).forEach { (base, actual) ->
            assertEquals(base.fontSize, actual.fontSize)
            assertEquals(base.lineHeight, actual.lineHeight)
        }
    }

    @Test
    fun `шрифт ложится на каждую роль`() {
        roles(typographyOf(Typeface.Mono, TextScale.Normal)).forEach { style ->
            assertEquals(FontFamily.Monospace, style.fontFamily)
        }
        roles(typographyOf(Typeface.Serif, TextScale.Large)).forEach { style ->
            assertEquals(FontFamily.Serif, style.fontFamily)
        }
    }

    @Test
    fun `незнакомый код даёт обычный шрифт и размер`() {
        assertEquals(Typeface.System, Typeface.byCode(null))
        assertEquals(Typeface.System, Typeface.byCode("comic"))
        assertEquals(TextScale.Normal, TextScale.byCode(null))
        assertEquals(TextScale.Normal, TextScale.byCode("huge"))
    }

    private fun roles(typography: Typography): List<TextStyle> = with(typography) {
        listOf(
            displayLarge, displayMedium, displaySmall,
            headlineLarge, headlineMedium, headlineSmall,
            titleLarge, titleMedium, titleSmall,
            bodyLarge, bodyMedium, bodySmall,
            labelLarge, labelMedium, labelSmall
        )
    }

    private fun assertNear(expected: Float, actual: Float, what: String) {
        assertTrue(abs(expected - actual) < 0.01f, "$what: ожидалось $expected, вышло $actual")
    }
}
