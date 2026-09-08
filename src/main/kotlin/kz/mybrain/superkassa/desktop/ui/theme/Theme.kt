package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Оформление кассы.
 *
 * Схема, шрифты и формы заданы целиком по Material 3 и живут в трёх
 * соседних файлах: `Palette`, `Typography`, здесь — формы и сборка.
 * Экраны берут только роли схемы; своих цветов у них нет.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Sizes.chipCorner),
    small = RoundedCornerShape(Sizes.smallCorner),
    medium = RoundedCornerShape(Sizes.corner),
    large = RoundedCornerShape(Sizes.largeCorner),
    extraLarge = RoundedCornerShape(Sizes.hugeCorner)
)

/**
 * Цвета, которых нет в ролях Material.
 *
 * Подложка под печатной лентой намеренно вне схемы: она изображает стол,
 * на котором лежит бумага, и не должна менять оттенок вместе с темой —
 * иначе белый чек теряется на светлом фоне.
 */
object AppColors {
    val backdrop = Color(0xFF2B2B2E)
}

/**
 * Цвет состояния документа.
 *
 * «Доставлено» и «ждёт» — свои цвета, по одному на тему: занимать ими
 * вторичную и третичную роли схемы нельзя, Material красит этими ролями
 * тональные кнопки и выбранные строки. «Отказ» — роль `error`: это
 * действительно ошибка, и роль для неё в схеме уже есть.
 */
object StatusColors {
    val delivered: Color
        @Composable get() = statuses().delivered

    val pending: Color
        @Composable get() = statuses().pending

    val refused: Color
        @Composable get() = MaterialTheme.colorScheme.error

    @Composable
    private fun statuses(): StatusPalette =
        if (LocalDarkTheme.current) DarkStatuses else LightStatuses
}

/**
 * Тёмная ли сейчас касса.
 *
 * Спрашивать систему второй раз нельзя: кассир мог выбрать тему сам,
 * и цвета состояний разошлись бы со схемой — светлая схема с тёмными
 * плашками.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Выбор кассира сильнее системного: он и есть выбор этого места. */
@Composable
private fun darkChosen(appearance: Appearance): Boolean = when (appearance) {
    Appearance.System -> isSystemInDarkTheme()
    Appearance.Light -> false
    Appearance.Dark -> true
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

@Composable
fun SuperkassaTheme(appearance: Appearance = Appearance.System, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkChosen(appearance)) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides scrollbar(),
            LocalDarkTheme provides darkChosen(appearance),
            content = content
        )
    }
}
