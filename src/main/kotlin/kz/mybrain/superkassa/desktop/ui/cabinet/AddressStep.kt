package kz.mybrain.superkassa.desktop.ui.cabinet

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
import kz.mybrain.superkassa.desktop.server.cabinet.AddressSuggestion
import kz.mybrain.superkassa.desktop.ui.components.onEscape
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Durations

/**
 * Текущий шаг: поле поиска и найденное выпадающим списком.
 *
 * Какой запрос регистр принимает, а какой отвергает, решает
 * [askableQuery] — правило общее с классификатором ОКЭД. На отвергнутом
 * запросе список не трогается: он остаётся с прошлого, а не сменяется
 * отказом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressStep(cabinet: CabinetSession, texts: CabinetTexts, path: AddressPath) {
    var query by remember(path.depth) { mutableStateOf("") }
    var found by remember(path.depth) { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var searched by remember(path.depth) { mutableStateOf(false) }
    var open by remember(path.depth) { mutableStateOf(false) }

    var touched by remember(path.depth) { mutableStateOf(false) }

    val needle = query.trim()
    val askable = askableQuery(needle)
    LaunchedEffect(needle, path.depth) {
        if (!askable) return@LaunchedEffect
        if (needle.isNotEmpty()) delay(Durations.afterTyping)
        val token = cabinet.token ?: return@LaunchedEffect
        found = distinctSuggestions(cabinet.guard { path.lookup(cabinet, token, needle) }.orEmpty())
        searched = true
        // Список показывается только тому, кто взялся за поле: при показе
        // экрана с готовой точкой он раскрывался сам поверх карточки.
        open = touched && found.isNotEmpty()
    }
    // Escape закрывает раскрытый список — тем же правилом, что и у всех
    // выпадающих списков приложения: сам он нажатия не слышит.
    val closing = Modifier.fillMaxWidth().onEscape {
        val wasOpen = open
        open = false
        wasOpen
    }
    ExposedDropdownMenuBox(
        expanded = open && found.isNotEmpty(),
        onExpandedChange = { open = it },
        modifier = closing
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                touched = true
                open = true
            },
            label = { Text(path.currentLabel(texts)) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open && found.isNotEmpty()) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        touched = true
                        open = found.isNotEmpty()
                    }
                }
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(expanded = open && found.isNotEmpty(), onDismissRequest = { open = false }) {
            found.forEach { suggestion ->
                DropdownMenuItem(
                    text = { SuggestionLabel(suggestion) },
                    onClick = {
                        open = false
                        path.choose(suggestion)
                    }
                )
            }
        }
    }
    // Пустой ответ говорится и на пустом запросе: с него шаг и начинается,
    // и молчащий регистр было не отличить от полного — поле просто стояло
    // пустым, а раскрытый список не показывал ничего.
    if (searched && found.isEmpty()) {
        Text(
            text = texts.addressNotFound,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Подсказка в списке: название, а у строения под ним код РКА.
 *
 * Регистр отдаёт на один номер дома две записи — «10» и «10», — и они
 * не двойники: у них разные идентификаторы и разные коды РКА (один
 * начинается с нуля, другой с двойки). Кабинет передаёт их как есть,
 * а различавшее их указание регистра («дом», «строение») до приложения
 * не доходит вовсе, поэтому в списке видно то единственное, чем записи
 * действительно различаются, — код РКА. Он же уходит в кабинет адресом
 * точки, так что владелец выбирает его осознанно.
 */
@Composable
private fun SuggestionLabel(suggestion: AddressSuggestion) {
    Column {
        Text(text = suggestion.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        suggestion.rka?.let { rka ->
            Text(
                text = rka,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Записи, различимые в списке.
 *
 * Полный двойник — то же название и тот же код РКА — из списка убирается:
 * выбрать из двух одинаковых строк владельцу нечего. Записи с общим
 * номером и разными кодами остаются: это разные адреса регистра.
 */
internal fun distinctSuggestions(items: List<AddressSuggestion>): List<AddressSuggestion> =
    items.distinctBy { it.name to it.rka }
