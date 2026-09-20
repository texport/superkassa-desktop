package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.server.cabinet.OkedEntry
import kz.mybrain.superkassa.desktop.server.cabinet.okedSuggestions
import kz.mybrain.superkassa.desktop.ui.components.onEscape
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Ввод вида деятельности: выбор из классификатора ОКЭД.
 *
 * Прежде код и наименование набирались руками, и в заявление в ИСНА уходило
 * написанное владельцем. Классификатор теперь держит кабинет, поэтому вид
 * деятельности выбирается из него: набранное ищется по коду и по части
 * наименования, а в компанию уходит ровно то, что стоит в классификаторе.
 *
 * Уже добавленный код в подсказках не показывается: такой список кабинет
 * отвергнет.
 *
 * @param known добавленные коды — их из подсказок убирают.
 */
@Composable
fun OkedPicker(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    known: List<String>,
    onAdd: (Oked) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<OkedEntry>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }

    val needle = query.trim()
    // Однобуквенный запрос кабинет отвергает, как и в адресном регистре:
    // ищем либо с пустой строки — она отдаёт начало классификатора, —
    // либо от двух знаков.
    val askable = askableQuery(needle)
    LaunchedEffect(needle) {
        if (!askable) return@LaunchedEffect
        val token = cabinet.token ?: return@LaunchedEffect
        if (needle.isNotEmpty()) delay(Durations.afterTyping)
        found = cabinet.guard { cabinet.client.okedSuggestions(token, needle).items }
            .orEmpty()
            .filterNot { it.code in known }
        searched = true
        open = touched && found.isNotEmpty()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Suggestions(
            texts = texts,
            query = query,
            found = found,
            open = open && found.isNotEmpty(),
            title = { entry -> titleOf(session, entry) },
            onOpen = { open = it },
            onQuery = {
                query = it
                touched = true
                open = true
            }
        ) { entry ->
            open = false
            query = ""
            onAdd(Oked(code = entry.code, name = titleOf(session, entry)))
        }
        val nothing = searched && found.isEmpty() && needle.isNotEmpty()
        Hint(if (nothing) texts.okedNotFound else texts.okedSearchHint)
    }
}

/** Поле поиска и найденное выпадающим списком: как в подборе адреса. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Suggestions(
    texts: CabinetTexts,
    query: String,
    found: List<OkedEntry>,
    open: Boolean,
    title: (OkedEntry) -> String,
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
        }
    }
}

/** Подсказка под полем: одна строка на все состояния, чтобы поле не прыгало. */
@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Наименование на языке интерфейса: в заявление уходит то, что видит владелец. */
internal fun titleOf(session: Session, entry: OkedEntry): String =
    addressIn(session.language, entry.name, entry.nameKz)
