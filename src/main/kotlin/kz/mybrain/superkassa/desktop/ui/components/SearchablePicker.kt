package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Выбор одного из длинного набора: с набором и списком совпадений.
 *
 * [LabelledPicker] раскрывает набор целиком, и это верно, пока в нём
 * несколько значений. В справочнике моделей касс их сотни, а владелец
 * знает, как называется его касса, — искать её глазами по списку он
 * не должен.
 *
 * Список раскрывается только тому, кто взялся за поле: иначе он падал бы
 * поверх соседних полей при показе формы. Пока после выбора ничего
 * не набрано, в списке весь набор — сужать его по уже выбранному
 * значению значило бы спрятать остальные.
 *
 * @param title как назвать значение в поле и в списке.
 * @param keys по чему искать: название и код — владелец набирает то, что
 *   помнит, а помнит он либо одно, либо другое.
 * @param notFound строка о том, что по набранному ничего нет.
 */
@Composable
fun <T> SearchablePicker(
    label: String,
    options: List<T>,
    selected: T?,
    title: (T) -> String,
    keys: (T) -> List<String>,
    notFound: String,
    onSelect: (T) -> Unit
) {
    var query by remember(selected) { mutableStateOf(selected?.let(title).orEmpty()) }
    var typed by remember(selected) { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    val found = if (typed) narrowed(options, query, keys) else options
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        Picker(
            label = label,
            query = query,
            found = found,
            open = open,
            title = title,
            onOpen = { open = it },
            onQuery = {
                query = it
                typed = true
                open = true
            }
        ) { picked ->
            open = false
            typed = false
            query = title(picked)
            onSelect(picked)
        }
        if (typed && found.isEmpty()) NotFoundLine(notFound)
    }
}

/** Поле с набором и раскрытым под ним списком совпадений. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Picker(
    label: String,
    query: String,
    found: List<T>,
    open: Boolean,
    title: (T) -> String,
    onOpen: (Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onPick: (T) -> Unit
) {
    // Пустой список не раскрывается: пустая рамка под полем читается
    // как отказ приложения, а не как «по этому ничего нет».
    val shown = open && found.isNotEmpty()
    // Escape закрывает раскрытый список — тем же правилом, что и у поля
    // выбора: пока закрывать нечего, нажатие уходит дальше, к диалогу.
    val closing = Modifier.fillMaxWidth().onEscape {
        if (shown) onOpen(false)
        shown
    }
    // Ширина поля нужна списку под ним: строки собираются по мере показа,
    // а такой список меряется не содержимым, а числом.
    var fieldWidth by remember { mutableStateOf(0.dp) }
    ExposedDropdownMenuBox(expanded = shown, onExpandedChange = onOpen, modifier = closing) {
        PickerField(
            label = label,
            query = query,
            open = shown,
            onQuery = onQuery,
            onWidth = { fieldWidth = it }
        ) { onOpen(true) }
        ExposedDropdownMenu(expanded = shown, onDismissRequest = { onOpen(false) }) {
            PickerRows(found, fieldWidth) { option ->
                DropdownMenuItem(
                    text = { Text(text = title(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onPick(option) }
                )
            }
        }
    }
}

/** Поле набора: то же оформление, что у остальных полей формы. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.PickerField(
    label: String,
    query: String,
    open: Boolean,
    onQuery: (String) -> Unit,
    onWidth: (Dp) -> Unit,
    onTaken: () -> Unit
) {
    val density = LocalDensity.current
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { placed -> onWidth(with(density) { placed.size.width.toDp() }) }
            .onFocusChanged { state -> if (state.isFocused) onTaken() }
            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
    )
}

/** По набранному ничего не нашлось — строкой под полем, а не пустым списком. */
@Composable
private fun NotFoundLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Что из набора подходит набранному.
 *
 * Набранное разбирается на слова, и подходит то, в чьих полях поиска
 * нашлось каждое слово — в любом порядке и без учёта регистра. Прежде
 * запрос искался целиком, и «1999 Абая» не находило «Магазин на Абая
 * 1999»: владелец набирал то, что помнит, в том порядке, в каком помнит,
 * и получал «ничего не нашлось» о своей же точке.
 *
 * Слово ищется вхождением, а не целым словом: «касса» находит
 * «Суперкасса», а начало кода — модель по коду. Слова вправе найтись
 * в разных полях — «абая 12» подходит точке с улицей в адресе и номером
 * в названии. Пустой запрос ничего не сужает: набор показывается целиком.
 */
fun <T> narrowed(options: List<T>, query: String, keys: (T) -> List<String>): List<T> {
    val words = query.split(WORD_BREAKS).filter { it.isNotBlank() }
    if (words.isEmpty()) return options
    return options.filter { option ->
        val fields = keys(option)
        words.all { word -> fields.any { it.contains(word, ignoreCase = true) } }
    }
}

/**
 * Чем набранное делится на слова.
 *
 * Не одним пробелом: адрес владелец набирает так, как его читает, —
 * «Абая, 12», — и запятая, прилипшая к слову, не должна мешать найти дом.
 */
private val WORD_BREAKS = Regex("[\\s,;]+")
