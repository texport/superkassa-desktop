package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.fieldLabelReserve

/**
 * Кнопка, стоящая в одной строке с полем ввода.
 *
 * Повторяет геометрию поля с подписью: тот же запас над рамкой
 * и тот же рост. Кнопка по умолчанию ниже поля и без запаса сверху,
 * и в строке «поле — кнопка» она стояла выше рамки на половину запаса.
 * Строка с такими кнопками выравнивается по верху: тогда строка
 * подсказки под полем не сдвигает кнопки вниз.
 *
 * Без поля рядом эта кнопка не нужна: там ставится обычная кнопка
 * Material 3.
 *
 * @param text подпись действия.
 * @param kind насколько действие главное: от него зависит заливка.
 */
@Composable
fun FieldButton(
    text: String,
    kind: FieldButtonKind = FieldButtonKind.Tonal,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shaped = modifier.besideField()
    when (kind) {
        FieldButtonKind.Filled -> Button(onClick, shaped, enabled) { Text(text) }
        FieldButtonKind.Tonal -> FilledTonalButton(onClick, shaped, enabled) { Text(text) }
        FieldButtonKind.Outlined -> OutlinedButton(onClick, shaped, enabled) { Text(text) }
        FieldButtonKind.Text -> TextButton(onClick, shaped, enabled) { Text(text) }
    }
}

/** Насколько действие главное на своём экране. */
enum class FieldButtonKind { Filled, Tonal, Outlined, Text }

/**
 * Геометрия поля с подписью для элемента, стоящего в строке с ним:
 * запас над рамкой и рост рамки.
 */
@Composable
fun Modifier.besideField(): Modifier = padding(top = fieldLabelReserve()).height(Sizes.fieldHeight)

/**
 * Только запас над рамкой, без роста: для элемента, который в строке
 * с полем центрируется по рамке и сохраняет свой размер.
 */
@Composable
fun Modifier.underFieldLabel(): Modifier = padding(top = fieldLabelReserve())
