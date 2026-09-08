package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.server.cabinet.OkedEntry
import kz.mybrain.superkassa.desktop.server.cabinet.okedReference
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Выбор вида деятельности из классификатора ОКЭД.
 *
 * Прежде код и наименование набирались руками. Классификатор ведёт
 * уполномоченный орган, ИСНА сверяется с ним же — и набранное владельцем
 * «47111 Магазин» уходило в заявление как есть, чтобы вернуться отказом.
 * Придумать наименование к коду тем более нельзя: в регистрационной карте
 * оно печатается тем, что записано в классификаторе.
 *
 * Список показывается сразу, до всякого ввода: у владельца нет под рукой
 * классификатора, чтобы вспомнить, с чего начать. Строка поиска отбирает
 * по коду, если набраны цифры, и по наименованию на любом из языков —
 * если слова.
 *
 * Уже добавленные виды из списка выпадают: кабинет второй такой же
 * не примет, а строка, которая ничего не делает, читается как поломка.
 */
@Composable
fun OkedPicker(cabinet: CabinetSession, texts: CabinetTexts, language: Language, known: List<String>, onAdd: (Oked) -> Unit) {
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<OkedEntry>>(emptyList()) }

    // Поиск идёт за набором, а не по кнопке: классификатор большой,
    // и владелец сужает список, дописывая слово. Пауза перед обращением
    // держит одно обращение на слово, а не на букву.
    LaunchedEffect(query, cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        if (query.isNotEmpty()) delay(TYPING_PAUSE_MS)
        found = cabinet.guard { cabinet.client.okedReference(token, query.trim()) }?.items.orEmpty()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(texts.okedSearch) },
            supportingText = { Text(texts.okedSearchHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val offered = found.filterNot { it.code in known }
        if (offered.isEmpty()) {
            Text(
                text = texts.okedNotFound,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        offered.forEachIndexed { at, entry ->
            RecordRow(
                title = okedName(entry, language),
                subtitle = entry.code,
                striped = at % STRIPE == 1,
                onClick = { onAdd(Oked(code = entry.code, name = okedName(entry, language))) }
            )
        }
    }
}

/**
 * Наименование вида деятельности на языке владельца.
 *
 * Уходит в кабинет вместе с кодом и печатается в регистрационной карте,
 * поэтому берётся из классификатора, а не составляется приложением.
 * Пустая казахская форма — обычное дело: справочник отдаёт её не всегда.
 */
fun okedName(entry: OkedEntry, language: Language): String = when (language) {
    Language.Kk -> entry.nameKz.takeIf { it.isNotBlank() } ?: entry.name
    else -> entry.name.takeIf { it.isNotBlank() } ?: entry.nameKz
}

/** Сколько ждать после последней набранной буквы, прежде чем искать. */
private const val TYPING_PAUSE_MS = 300L

/** Затеняется каждая вторая строка списка. */
private const val STRIPE = 2
