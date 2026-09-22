package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Выбор одного из списка — для наборов, которых нет в справочнике узла.
 *
 * [DictionaryPicker] работает с трёхъязычным справочником узла; здесь
 * набор приходит от другой службы и названия у него свои. Один и тот же
 * выпадающий список на все такие случаи, чтобы поля выглядели одинаково
 * на всех экранах.
 *
 * @param title как назвать значение; `null` означает «ничего не выбрано».
 * @param available принимает ли узел это значение сейчас. Непринимаемое
 * не прячется, а гаснет: спрятать его значило бы разойтись с узлом молча.
 * @param width заданная ширина поля. При ней длинное название обрезается
 * многоточием: поле только читается, и вторую строку обрезала бы рамка.
 * @param create чем заводят то, чего в списке ещё нет; `null` — нечем.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LabelledPicker(
    label: String,
    options: List<T>,
    selected: T?,
    title: (T?) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    available: (T) -> Boolean = { true },
    width: Dp? = null,
    create: PickerCreate? = null
) {
    var open by remember { mutableStateOf(false) }
    // Ширину поля меряет сама разметка: поле бывает и во всю ширину формы,
    // и заданной ширины, а список под ним идёт с ним по одному краю.
    var fieldWidth by remember { mutableStateOf(width ?: 0.dp) }
    val density = LocalDensity.current
    val sized = if (width == null) modifier.fillMaxWidth() else modifier.width(width)
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        // Escape закрывает раскрытый список: сам он нажатия не слышит,
        // потому что список раскрывается в окне без фокуса.
        modifier = sized.onEscape {
            val wasOpen = open
            open = false
            wasOpen
        }
    ) {
        OutlinedTextField(
            value = if (width == null) title(selected) else shortened(title(selected), width),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = (if (width == null) Modifier.fillMaxWidth() else Modifier)
                .onGloballyPositioned { placed ->
                    fieldWidth = with(density) { placed.size.width.toDp() }
                }
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            create?.let { adding ->
                DropdownMenuItem(
                    leadingIcon = { Icon(AppIcons.add, contentDescription = null) },
                    text = { Text(adding.title) },
                    onClick = {
                        open = false
                        adding.onCreate()
                    }
                )
                if (options.isNotEmpty()) HorizontalDivider()
            }
            if (options.isEmpty() && create == null) EmptyPickerLine()
            PickerRows(options, fieldWidth) { option ->
                DropdownMenuItem(
                    enabled = available(option),
                    text = { Text(text = title(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}

/**
 * Пустой список — строкой, а не пустой рамкой.
 *
 * Раскрытая пустота читается как сбой приложения: справочник узел мог
 * не отдать вовсе, и сказать об этом словами дешевле, чем заставлять
 * владельца гадать.
 */
@Composable
private fun EmptyPickerLine() {
    DropdownMenuItem(
        enabled = false,
        text = { Text(LocalStrings.current.common.nothingToPick) },
        onClick = {}
    )
}

/**
 * Название, укороченное до ширины поля.
 *
 * Меряется тем же шрифтом, которым поле рисует текст: обрезка «на глазок»
 * по числу букв на пропорциональном шрифте промахивается то в одну,
 * то в другую сторону.
 */
@Composable
private fun shortened(text: String, width: Dp): String {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyLarge
    val density = LocalDensity.current
    // Перебор букв — десятки раскладок шрифта на одно название, и делать
    // его заново на каждую перерисовку нельзя: поле само сообщает свою
    // ширину при каждой разметке, а при растягивании окна разметка идёт
    // десятки раз в секунду.
    return remember(text, width, style, density) {
        val available = with(density) { (width - Sizes.fieldTextInset).toPx() }
        if (measurer.measure(text, style).size.width <= available) {
            text
        } else {
            var fits = 0
            for (length in 1..text.length) {
                val candidate = text.take(length) + Glyphs.ELLIPSIS
                if (measurer.measure(candidate, style).size.width > available) break
                fits = length
            }
            text.take(fits).trimEnd() + Glyphs.ELLIPSIS
        }
    }
}
