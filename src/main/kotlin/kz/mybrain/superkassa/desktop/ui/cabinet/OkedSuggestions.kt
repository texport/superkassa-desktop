package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.cabinet.OkedEntry
import kz.mybrain.superkassa.desktop.ui.components.onEscape
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Поле поиска по классификатору и найденное выпадающим списком.
 *
 * Отдельно от [OkedPicker]: тот держит состояние поиска — страницы,
 * запрос и состояние «уже спрашивали», — а здесь только показ. Разделено
 * потому, что вместе файл перестал читаться сверху вниз.
 *
 * Собрано так же, как подбор адреса: одно и то же дело в кабинете
 * не должно выглядеть двумя разными полями.
 *
 * @param rest сколько классификатор держит сверх показанного; `null` —
 *   продолжения нет, и просить его нечем.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OkedSuggestions(
    texts: CabinetTexts,
    query: String,
    found: List<OkedEntry>,
    open: Boolean,
    rest: Long?,
    title: (OkedEntry) -> String,
    onMore: () -> Unit,
    onOpen: (Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onPick: (OkedEntry) -> Unit
) {
    // Escape закрывает раскрытый список — тем же правилом, что и у всех
    // выпадающих списков приложения: сам он нажатия не слышит.
    val closing = Modifier.fillMaxWidth().onEscape {
        if (open) onOpen(false)
        open
    }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = onOpen, modifier = closing) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text(texts.okedSearch) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state -> if (state.isFocused) onOpen(true) }
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { onOpen(false) }) {
            found.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${entry.code}${Glyphs.SEPARATOR}${title(entry)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = { onPick(entry) }
                )
            }
            // Классификатор больше страницы, и список раскрыт внутрь меню:
            // по прокрутке догружать некуда, поэтому продолжение просят
            // последней строкой списка.
            if (rest != null) {
                val label = if (rest > 0) "${texts.showMore}${Glyphs.SEPARATOR}$rest" else texts.showMore
                DropdownMenuItem(text = { Text(label) }, onClick = onMore)
            }
        }
    }
}

/** Подсказка под полем: одна строка на все состояния, чтобы поле не прыгало. */
@Composable
internal fun OkedHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
