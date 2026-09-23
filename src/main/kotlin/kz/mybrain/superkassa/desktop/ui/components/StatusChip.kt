package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Плашка состояния.
 *
 * Собрана из ролей схемы: цвет роли на её же контейнере. Material для
 * такого случая предлагает `AssistChip`, но он рассчитан на нажатие
 * и тянет за собой обводку и высоту кнопки — здесь же надпись, которую
 * читают, а не нажимают.
 *
 * @param style ступень шрифта плашки. По умолчанию — та, что у записи
 *   списка; в строке таблицы задаётся ступень её клеток, иначе плашка
 *   оказывается крупнее всего ряда и на крупном шрифте рвёт слово
 *   пополам, пока соседние столбцы стоят свободно.
 */
@Composable
fun Chip(text: String, color: Color, style: TextStyle = MaterialTheme.typography.labelMedium) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = CHIP_TINT))
            .padding(horizontal = Spacing.tight, vertical = Spacing.hairline)
    )
}

/** Насколько цвет плашки разбавлен фоном. */
private const val CHIP_TINT = 0.16f
