package kz.mybrain.superkassa.presentation.common.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Поле окна для этого окна — по его классу ширины (см. [Spacing.windowMargin]).
 *
 * Ставит его каркас окна один раз вокруг раздела; шапка начинает заголовок
 * с него же, и начало заголовка совпадает с краем содержимого под ним.
 */
val windowMargin: Dp
    @Composable get() = Spacing.windowMargin(LocalWindowClass.current.width == WidthClass.Compact)
