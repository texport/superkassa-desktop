package kz.mybrain.superkassa.presentation.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Полоса прокрутки настольной кассы — внутри общей темы. */
@Composable
internal actual fun PlatformTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalScrollbarStyle provides scrollbar(), content = content)
}

/**
 * Полоса прокрутки.
 *
 * Своя, потому что стандартная почти невидима на тёмной поверхности:
 * кассир не находил список видов оплаты, уходивший за нижний край панели.
 * Цвет берётся ролями схемы и потому одинаково читается в обеих темах.
 */
@Composable
private fun scrollbar(): ScrollbarStyle = defaultScrollbarStyle().copy(
    thickness = Sizes.scrollbar,
    shape = MaterialTheme.shapes.extraSmall,
    unhoverColor = MaterialTheme.colorScheme.outlineVariant,
    hoverColor = MaterialTheme.colorScheme.outline
)
