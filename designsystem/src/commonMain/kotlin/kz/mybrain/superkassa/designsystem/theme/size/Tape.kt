package kz.mybrain.superkassa.designsystem.theme.size

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Показ печатной ленты.
 *
 * Лента Z-отчёта — тысячи точек по высоте, поэтому окно берёт весь
 * экран, а ширину ленты кассир меняет сам: узкая читается как бумага,
 * широкая даёт разобрать суммы.
 */
object Tape {
    /** Ширина ленты при открытии. */
    val defaultWidth = 130.steps

    /** Пределы и шаг переключения ширины. */
    val minWidth = 80.steps
    val maxWidth = 285.steps
    val widthStep = 25.steps

    /** Поле сверху и снизу ленты внутри окна. */
    val margin = Spacing.sectionGap
}

/**
 * Сколько места поле с подписью держит над рамкой.
 *
 * `OutlinedTextField` Material 3 поднимает подпись на рамку и заранее
 * оставляет над рамкой половину строки `bodySmall`, поэтому поле выше своей
 * рамки, а рамка прижата к его низу. Элемент, стоящий в строке с полем,
 * берёт тот же запас сверху: иначе он центрируется по всему полю и стоит
 * выше рамки на половину запаса.
 */
@Composable
fun fieldLabelReserve(): Dp = with(LocalDensity.current) {
    MaterialTheme.typography.bodySmall.lineHeight.toDp() / 2
}
