package kz.mybrain.superkassa.designsystem.list

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Строка списка от края до края панели внутри группы с полями.
 *
 * Группа настроек отступает от краёв панели на поле строки списка, чтобы
 * её подзаголовок, поля и кнопки начинались с той же вертикали, что
 * подписи строк. Сама строка Material 3 держит это поле внутри себя,
 * а её подложка и отклик нажатия идут по всей ширине — строка выходит
 * за поле группы ровно на [by] с каждой стороны.
 */
internal fun Modifier.bleed(by: Dp): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val side = by.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + side * 2, maxWidth = constraints.maxWidth + side * 2)
    )
    layout(placeable.width - side * 2, placeable.height) { placeable.place(-side, 0) }
}
