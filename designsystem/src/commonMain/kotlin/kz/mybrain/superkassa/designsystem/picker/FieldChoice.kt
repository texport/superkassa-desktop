package kz.mybrain.superkassa.designsystem.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.button.underFieldLabel
import kz.mybrain.superkassa.designsystem.theme.size.Sizes

/**
 * Выбор способа ввода поля — сегментами Material 3 в строке с полем,
 * например тенге или процент у скидки.
 *
 * Рядом с полем, а не внутри него: в слоте значка поля сегменты упирались
 * в его рамку без всякого поля, а Material 3 кладёт туда значок, а не
 * элемент управления. Сегменты повторяют геометрию поля с подписью — тот
 * же запас над рамкой, по центру её роста, — как кнопка рядом с полем.
 * Строка «поле — выбор» выравнивается по верху: строка подсказки под
 * полем не сдвигает выбор вниз.
 */
@Composable
fun <T> FieldChoice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Box(modifier = Modifier.underFieldLabel().height(Sizes.fieldHeight), contentAlignment = Alignment.Center) {
        ChoiceSegments(options = options, selected = selected, label = label, onSelect = onSelect)
    }
}
