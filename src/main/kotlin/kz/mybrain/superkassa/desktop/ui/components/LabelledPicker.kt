package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * Выбор одного из списка — для наборов, которых нет в справочнике узла.
 *
 * [DictionaryPicker] работает с трёхъязычным справочником узла; здесь
 * набор приходит от другой службы и названия у него свои. Один и тот же
 * выпадающий список на все такие случаи, чтобы поля выглядели одинаково
 * на всех экранах.
 *
 * @param title как назвать значение; `null` означает «ничего не выбрано».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LabelledPicker(
    label: String,
    options: List<T>,
    selected: T?,
    title: (T?) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = title(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(text = title(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    onClick = {
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}
