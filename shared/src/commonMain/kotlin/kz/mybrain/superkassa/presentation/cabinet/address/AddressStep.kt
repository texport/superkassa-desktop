package kz.mybrain.superkassa.presentation.cabinet.address

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
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.presentation.common.keyboard.onEscape
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts

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
internal fun AddressStep(
    texts: CabinetTexts,
    path: AddressPath,
    onQuery: (String) -> Unit,
    onPick: (AddressSuggestion) -> Unit
) {
    // Раскрыт ли список и брался ли владелец за поле — дело самого поля:
    // при показе экрана с готовой точкой список раскрывался сам поверх карточки.
    var open by remember(path.depth) { mutableStateOf(false) }
    val shown = open && path.found.isNotEmpty()
    // Escape закрывает раскрытый список — тем же правилом, что и у всех
    // выпадающих списков приложения: сам он нажатия не слышит.
    val closing = Modifier.fillMaxWidth().onEscape {
        val wasOpen = open
        open = false
        wasOpen
    }
    ExposedDropdownMenuBox(expanded = shown, onExpandedChange = { open = it }, modifier = closing) {
        StepField(texts, path, shown, onFocus = { open = path.found.isNotEmpty() }) {
            onQuery(it)
            open = true
        }
        ExposedDropdownMenu(expanded = shown, onDismissRequest = { open = false }) {
            path.found.forEach { suggestion ->
                DropdownMenuItem(text = { SuggestionLabel(suggestion) }, onClick = { onPick(suggestion) })
            }
        }
    }
    // Пустой ответ говорится и на пустом запросе: с него шаг и начинается,
    // и молчащий регистр было не отличить от полного — поле просто стояло
    // пустым, а раскрытый список не показывал ничего.
    if (path.searched && path.found.isEmpty()) NotFound(texts)
}

/** Регистр на этом шаге ничего не нашёл. */
@Composable
private fun NotFound(texts: CabinetTexts) {
    Text(
        text = texts.addressNotFound,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Поле шага: подпись называет, что именно выбирается сейчас. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.StepField(
    texts: CabinetTexts,
    path: AddressPath,
    shown: Boolean,
    onFocus: () -> Unit,
    onQuery: (String) -> Unit
) {
    OutlinedTextField(
        value = path.query,
        onValueChange = onQuery,
        label = { Text(path.currentLabel(texts)) },
        singleLine = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shown) },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state -> if (state.isFocused) onFocus() }
            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
    )
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
