package kz.mybrain.superkassa.presentation.common.picker

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.theme.size.Sizes

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
    val width = segmentWidth(options.map(label))
    SegmentRow(options, selected, label, modifier, { enabled && available(it) }, onSelect) { Modifier.width(width) }
}

/**
 * Те же сегменты во всю ширину карточки или формы: делят её поровну.
 *
 * Выбор внутри карточки стоит под своей подписью и занимает строку целиком,
 * как поле формы над ним. Уже самой длинной подписи сегмент не становится:
 * в узкой карточке ряд упирается в край, а не обрывает подпись.
 */
@Composable
fun <T> WideChoiceSegments(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    enabled: Boolean = true,
    available: (T) -> Boolean = { true },
    onSelect: (T) -> Unit
) {
    val least = segmentWidth(options.map(label)) * options.size
    val row = Modifier.fillMaxWidth().widthIn(min = least)
    SegmentRow(options, selected, label, row, { enabled && available(it) }, onSelect) { Modifier.weight(1f) }
}

/**
 * Ширина сегмента по самой длинной подписи набора, а не числом: у сегментов
 * Material внутренняя ширина не зависит от подписи, и «Полная страница»
 * обрезалась до «Полная стра». Единая ширина на все сегменты держит ряд ровным.
 *
 * Считается один раз на набор подписей: при растягивании окна разметка
 * пересчитывается десятки раз в секунду, и раскладка шрифта на каждый
 * такой проход — работа впустую.
 */
@Composable
private fun segmentWidth(labels: List<String>): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    return remember(labels, style, density) {
        val widest = labels.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0
        with(density) { widest.toDp() } + Sizes.segmentInset
    }
}

/** Ряд сегментов; ширину сегмента задаёт [segment]. */
@Composable
private fun <T> SegmentRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier,
    available: (T) -> Boolean,
    onSelect: (T) -> Unit,
    segment: RowScope.() -> Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { at, option ->
            SegmentedButton(
                selected = option == selected,
                enabled = available(option),
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(at, options.size),
                modifier = segment(),
                // Подпись, которой не хватило сегмента, кончается многоточием,
                // а не обрывается посреди слова: «Предупреждени» читалось
                // как опечатка.
                label = { Text(text = label(option), softWrap = false, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}
