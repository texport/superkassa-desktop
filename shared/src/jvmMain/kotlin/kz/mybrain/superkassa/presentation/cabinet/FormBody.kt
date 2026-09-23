package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.components.ScrollableColumn
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Поля окна заведения, которые прокручиваются, когда окну тесно.
 *
 * Окно ростом с содержимое упиралось в края окна кассы: в 960×640
 * на казахском и крупной ступени шрифта последнее поле кассы — её
 * название — уходило под кнопки окна, и дойти до него было нечем.
 * Шаги адреса точки прибавляют по полю на каждый выбор и тоже
 * вырастали за край. Теперь поля берут столько высоты, сколько
 * осталось от заголовка и кнопок окна, а лишнее прокручивают с видимой
 * полосой; кнопки окна всегда на месте.
 *
 * Промежуток между полями тот же, что у самого окна.
 */
@Composable
internal fun ColumnScope.FormBody(content: @Composable ColumnScope.() -> Unit) {
    ScrollableColumn(
        modifier = Modifier.weight(1f, fill = false),
        spacing = Spacing.snug,
        content = content
    )
}
