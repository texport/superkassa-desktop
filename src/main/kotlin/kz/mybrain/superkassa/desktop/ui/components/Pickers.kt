package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
/**
 * Выбор значения из справочника узла.
 *
 * Названия ОФД, контуров и ролей приходят с узла сразу на трёх языках.
 * Свой список в приложении означал бы, что при добавлении нового ОФД
 * кассир его не увидит, а при переименовании старого прочтёт не то,
 * что напечатано в чеке.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryPicker(
    label: String,
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit,
    width: Dp
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        modifier = Modifier.width(width)
    ) {
        val chosen = entries.firstOrNull { it.code == selectedCode }?.title(language) ?: selectedCode
        OutlinedTextField(
            // Поле только читается, и длинное название ОФД в нём переносилось
            // на вторую строку, которую обрезала рамка. Обрезаем сами и ставим
            // многоточие: кассир видит начало названия и знает, что оно длиннее.
            value = shortened(chosen, width),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.title(language), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(entry.code)
                        open = false
                    }
                )
            }
        }
    }
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
    val available = with(LocalDensity.current) { (width - Sizes.fieldTextInset).toPx() }
    if (measurer.measure(text, style).size.width <= available) return text
    var fits = 0
    for (length in 1..text.length) {
        val candidate = text.take(length) + ELLIPSIS
        if (measurer.measure(candidate, style).size.width > available) break
        fits = length
    }
    return text.take(fits).trimEnd() + ELLIPSIS
}

/** Знак того, что название длиннее поля. */
private const val ELLIPSIS = "…"

/** Выбор ОФД. */
@Composable
fun ProviderPicker(
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit
) = DictionaryPicker(
    label = LocalStrings.current.settings.ofd,
    entries = entries,
    language = language,
    selectedCode = selectedCode,
    onSelect = onSelect,
    width = Sizes.fieldChoice
)

/** Выбор контура: стенд, тестовый или промышленный. */
@Composable
fun EnvironmentPicker(
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit
) = DictionaryPicker(
    label = LocalStrings.current.settings.environment,
    entries = entries,
    language = language,
    selectedCode = selectedCode,
    onSelect = onSelect,
    width = Sizes.fieldChoice
)
