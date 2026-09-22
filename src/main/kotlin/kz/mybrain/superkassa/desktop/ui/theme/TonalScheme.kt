package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Схема Material 3, выведенная из одного оттенка.
 *
 * Роли берут те же тона, что и в эталонной схеме индиго: главная роль —
 * тон 40 на светлом и 80 на тёмном, контейнер — 90 и 30, надпись на
 * контейнере — 10 и 90, поверхности — почти нейтральные с тем же оттенком.
 * Вторичная роль — тот же оттенок с малой насыщенностью, третичная —
 * оттенок, сдвинутый по кругу, как того требует палитра Material.
 *
 * Роль ошибки одна на все тона: отказ остаётся красным при любом акценте,
 * иначе красная касса перестала бы отличать отказ от своего же цвета.
 */
internal fun tonalScheme(hue: Float, dark: Boolean): ColorScheme {
    val accent = Palette(hue, ACCENT_CHROMA)
    val companion = Palette(hue + SECONDARY_SHIFT, SECONDARY_CHROMA)
    val rare = Palette(hue + TERTIARY_SHIFT, TERTIARY_CHROMA)
    val neutral = Palette(hue, NEUTRAL_CHROMA)
    val variant = Palette(hue, VARIANT_CHROMA)
    return if (dark) {
        darkOf(accent, companion, rare, neutral, variant)
    } else {
        lightOf(accent, companion, rare, neutral, variant)
    }
}

/** Оттенок с насыщенностью: из него берутся тона. */
private class Palette(private val hue: Float, private val chroma: Float) {
    /**
     * Пастельные тона просят меньшую насыщенность: в Lab полная
     * насыщенность на тонах 80–90 у зелёного и бирюзового выходит
     * кислотной, а Material такие контейнеры держит спокойными.
     */
    fun shade(tone: Int): Color = Tones.of(hue, chroma.coerceAtMost(ceiling(tone)), tone)

    private fun ceiling(tone: Int): Float = when {
        tone >= LIGHTEST_TONES -> LIGHTEST_CHROMA
        tone >= LIGHT_TONES -> LIGHT_CHROMA
        else -> ACCENT_CHROMA
    }
}

private fun lightOf(accent: Palette, companion: Palette, rare: Palette, neutral: Palette, variant: Palette) =
    lightColorScheme(
        primary = accent.shade(tone = 40), onPrimary = accent.shade(tone = 100),
        primaryContainer = accent.shade(tone = 90), onPrimaryContainer = accent.shade(tone = 10),
        inversePrimary = accent.shade(tone = 80),
        secondary = companion.shade(tone = 40), onSecondary = companion.shade(tone = 100),
        secondaryContainer = companion.shade(tone = 90), onSecondaryContainer = companion.shade(tone = 10),
        tertiary = rare.shade(tone = 40), onTertiary = rare.shade(tone = 100),
        tertiaryContainer = rare.shade(tone = 90), onTertiaryContainer = rare.shade(tone = 10),
        error = ErrorLight.error, onError = ErrorLight.onError,
        errorContainer = ErrorLight.container, onErrorContainer = ErrorLight.onContainer,
        background = neutral.shade(tone = 98), onBackground = neutral.shade(tone = 10),
        surface = neutral.shade(tone = 98), onSurface = neutral.shade(tone = 10),
        surfaceVariant = variant.shade(tone = 90), onSurfaceVariant = variant.shade(tone = 30),
        surfaceContainerLowest = neutral.shade(tone = 100), surfaceContainerLow = neutral.shade(tone = 96),
        surfaceContainer = neutral.shade(tone = 94), surfaceContainerHigh = neutral.shade(tone = 92),
        surfaceContainerHighest = neutral.shade(tone = 90),
        outline = variant.shade(tone = 50), outlineVariant = variant.shade(tone = 80),
        inverseSurface = neutral.shade(tone = 20), inverseOnSurface = neutral.shade(tone = 95),
        scrim = neutral.shade(tone = 0)
    )

private fun darkOf(accent: Palette, companion: Palette, rare: Palette, neutral: Palette, variant: Palette) =
    darkColorScheme(
        primary = accent.shade(tone = 80), onPrimary = accent.shade(tone = 20),
        primaryContainer = accent.shade(tone = 30), onPrimaryContainer = accent.shade(tone = 90),
        inversePrimary = accent.shade(tone = 40),
        secondary = companion.shade(tone = 80), onSecondary = companion.shade(tone = 20),
        secondaryContainer = companion.shade(tone = 30), onSecondaryContainer = companion.shade(tone = 90),
        tertiary = rare.shade(tone = 80), onTertiary = rare.shade(tone = 20),
        tertiaryContainer = rare.shade(tone = 30), onTertiaryContainer = rare.shade(tone = 90),
        error = ErrorDark.error, onError = ErrorDark.onError,
        errorContainer = ErrorDark.container, onErrorContainer = ErrorDark.onContainer,
        background = neutral.shade(tone = 6), onBackground = neutral.shade(tone = 90),
        surface = neutral.shade(tone = 6), onSurface = neutral.shade(tone = 90),
        surfaceVariant = variant.shade(tone = 30), onSurfaceVariant = variant.shade(tone = 80),
        surfaceContainerLowest = neutral.shade(tone = 4), surfaceContainerLow = neutral.shade(tone = 10),
        surfaceContainer = neutral.shade(tone = 12), surfaceContainerHigh = neutral.shade(tone = 17),
        surfaceContainerHighest = neutral.shade(tone = 22),
        outline = variant.shade(tone = 60), outlineVariant = variant.shade(tone = 30),
        inverseSurface = neutral.shade(tone = 90), inverseOnSurface = neutral.shade(tone = 20),
        scrim = neutral.shade(tone = 0)
    )

/** Роль ошибки: цвета Material 3, одни на все тона. */
internal class ErrorRoles(val error: Color, val onError: Color, val container: Color, val onContainer: Color)

internal val ErrorLight = ErrorRoles(
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    container = Color(0xFFF9DEDC),
    onContainer = Color(0xFF410E0B)
)

internal val ErrorDark = ErrorRoles(
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    container = Color(0xFF8C1D18),
    onContainer = Color(0xFFF9DEDC)
)

/** Насыщенность главной роли: такая у эталонного индиго на тоне 40. */
private const val ACCENT_CHROMA = 60f
private const val SECONDARY_CHROMA = 13f
private const val TERTIARY_CHROMA = 21f
private const val NEUTRAL_CHROMA = 4f
private const val VARIANT_CHROMA = 6f

/** Сдвиг оттенка спутника и редкого акцента по кругу Lab — как у индиго. */
private const val SECONDARY_SHIFT = -6f
private const val TERTIARY_SHIFT = 42f

private const val LIGHT_TONES = 80
private const val LIGHT_CHROMA = 34f
private const val LIGHTEST_TONES = 90
private const val LIGHTEST_CHROMA = 22f
