package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Показ печатной ленты.
 *
 * Лента Z-отчёта — тысячи точек по высоте, поэтому окно берёт почти весь
 * экран, а ширину ленты кассир меняет сам: узкая читается как бумага,
 * широкая даёт разобрать суммы.
 */
object Tape {
    /** Доля окна под диалог печатной формы. */
    const val DIALOG_FRACTION = 0.92f

    /** Ширина ленты при открытии. */
    val defaultWidth = 520.dp

    /** Пределы и шаг переключения ширины. */
    val minWidth = 320.dp
    val maxWidth = 1140.dp
    val widthStep = 100.dp

    /** Поле сверху и снизу ленты внутри окна. */
    val margin = 24.dp
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
