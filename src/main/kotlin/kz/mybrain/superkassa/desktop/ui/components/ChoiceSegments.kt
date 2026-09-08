package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Выбор одного из нескольких — сегментами Material 3.
 *
 * Отдельные сегменты гаснут: набор один, а к нынешнему состоянию подходит
 * не всякий из них — заявление о постановке на учёт для кассы, уже стоящей
 * на учёте, кабинет отвергнет. Погашенный сегмент остаётся на месте:
 * спрятанный, он не объясняет, куда делся выбор.
 *
 * Заведено один раз: подпись сегмента не переносится ни при каком языке.
 * Material переносит её по умолчанию, и «Сатып алу» вставало в две строки,
 * разрывая ряд по высоте. По Material 3 подпись сегмента обязана
 * помещаться целиком — значит, шире должен становиться сегмент, а не
 * выше строка.
 */
@Composable
fun <T> ChoiceSegments(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    available: (T) -> Boolean = { true },
    onSelect: (T) -> Unit
) {
    // Ширина сегмента считается по самой длинной подписи набора, а не
    // берётся числом: у сегментов Material внутренняя ширина не зависит
    // от подписи, и «Полная страница» обрезалась до «Полная стра». Единая
    // ширина на все сегменты держит ряд ровным.
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val widest = options.maxOfOrNull { measurer.measure(label(it), style).size.width } ?: 0
    val segment = with(LocalDensity.current) { widest.toDp() } + Sizes.segmentInset
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { at, option ->
            SegmentedButton(
                selected = option == selected,
                enabled = enabled && available(option),
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(at, options.size),
                modifier = Modifier.width(segment),
                label = { Text(text = label(option), softWrap = false, maxLines = 1) }
            )
        }
    }
}
