package kz.mybrain.superkassa.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.theme.Accent
import kz.mybrain.superkassa.desktop.ui.theme.DarkStatuses
import kz.mybrain.superkassa.desktop.ui.theme.LightStatuses
import kz.mybrain.superkassa.desktop.ui.theme.schemeOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Надпись читается на своей подложке при любом тоне и любой теме.
 *
 * Схема выводится из оттенка расчётом, и промах в расчёте не падает
 * сборкой: он выходит серой надписью на серой плашке у одного тона
 * из четырнадцати — того, который владелец включит не сегодня.
 * Проверяется та же мерка, по которой Material 3 назначает ролям тона:
 * отношение контраста по WCAG.
 */
class LookContrastTest {

    @Test
    fun `надпись на заливке и на контейнере читается при любом тоне`() {
        eachScheme { accent, dark, scheme ->
            pairs(scheme).forEach { (role, colors) ->
                val ratio = LookColors.contrast(colors.first, colors.second)
                assertTrue(ratio >= TEXT, "$accent dark=$dark: $role даёт контраст $ratio")
            }
        }
    }

    /**
     * Обводка поля и заливка кнопки видны на странице.
     *
     * Разделитель `outlineVariant` сюда не входит намеренно: Material 3
     * держит его нарочно слабым, он отделяет строки, а не несёт смысла.
     */
    @Test
    fun `обводка и знак отличимы от поверхности при любом тоне`() {
        eachScheme { accent, dark, scheme ->
            listOf(
                "outline" to (scheme.outline to scheme.surface),
                "primary" to (scheme.primary to scheme.surface)
            ).forEach { (role, colors) ->
                val ratio = LookColors.contrast(colors.first, colors.second)
                assertTrue(ratio >= MARK, "$accent dark=$dark: $role даёт контраст $ratio")
            }
        }
    }

    /**
     * Смысловые цвета не следуют за тоном и не сливаются друг с другом.
     *
     * «Доставлено», «ждёт» и «отказ» кассир различает не по подписи,
     * а по цвету, и цвет у них один на все тона: выведи их из оттенка —
     * и на зелёной кассе ожидание стало бы зелёным.
     */
    @Test
    fun `состояния документа узнаваемы при любом тоне`() {
        eachScheme { accent, dark, scheme ->
            val statuses = if (dark) DarkStatuses else LightStatuses
            val marks = listOf(
                "доставлено" to statuses.delivered,
                "ждёт" to statuses.pending,
                "отказ" to scheme.error
            )
            marks.forEach { (name, color) ->
                val ratio = LookColors.contrast(color, scheme.surface)
                assertTrue(ratio >= TEXT, "$accent dark=$dark: $name на поверхности даёт $ratio")
            }
            apart(marks, accent, dark)
        }
    }

    private fun apart(marks: List<Pair<String, Color>>, accent: Accent, dark: Boolean) {
        marks.forEachIndexed { at, (name, color) ->
            marks.drop(at + 1).forEach { (other, second) ->
                val gap = LookColors.distance(color, second)
                assertTrue(gap >= STATUS_GAP, "$accent dark=$dark: $name и $other сошлись, ΔE $gap")
            }
        }
    }

    private fun pairs(scheme: ColorScheme): List<Pair<String, Pair<Color, Color>>> = with(scheme) {
        listOf(
            "primary" to (onPrimary to primary),
            "primaryContainer" to (onPrimaryContainer to primaryContainer),
            "secondary" to (onSecondary to secondary),
            "secondaryContainer" to (onSecondaryContainer to secondaryContainer),
            "tertiary" to (onTertiary to tertiary),
            "tertiaryContainer" to (onTertiaryContainer to tertiaryContainer),
            "error" to (onError to error),
            "errorContainer" to (onErrorContainer to errorContainer),
            "surface" to (onSurface to surface),
            "background" to (onBackground to background),
            "surfaceVariant" to (onSurfaceVariant to surfaceVariant),
            "surfaceContainerHighest" to (onSurface to surfaceContainerHighest),
            "surfaceContainerLowest" to (onSurface to surfaceContainerLowest),
            "inverseSurface" to (inverseOnSurface to inverseSurface)
        )
    }

    private fun eachScheme(check: (Accent, Boolean, ColorScheme) -> Unit) {
        Accent.entries.forEach { accent ->
            listOf(false, true).forEach { dark -> check(accent, dark, schemeOf(accent, dark)) }
        }
    }

    private companion object {
        /** Надпись на подложке — порог WCAG AA для обычного текста. */
        const val TEXT = 4.5f

        /** Обводка, значок, столбик графика — порог для нетекстового. */
        const val MARK = 3f

        /** Насколько состояния расходятся для глаза: порог различения — около двойки. */
        const val STATUS_GAP = 20f
    }
}
