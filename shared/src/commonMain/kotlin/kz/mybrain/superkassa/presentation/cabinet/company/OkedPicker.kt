package kz.mybrain.superkassa.presentation.cabinet.company

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.OkedEntry
import kz.mybrain.superkassa.presentation.cabinet.addressIn
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
 * Состояние поиска — страницы, запрос и «уже спрашивали» — держит модель
 * компании; здесь только поле и раскрытый список.
 */
@Composable
fun OkedPicker(
    search: OkedSearch,
    language: Language,
    texts: CabinetTexts,
    actions: CompanyActions,
    onAdd: (OkedEntry, String) -> Unit
) {
    // Раскрыт ли список и брался ли владелец за поле — дело самого поля:
    // при показе раздела список не раскрывается сам поверх карточки.
    var open by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        OkedSuggestionsField(
            texts = texts,
            query = search.query,
            found = search.found,
            open = open && search.found.isNotEmpty(),
            rest = search.rest,
            title = { entry -> titleOf(language, entry) },
            onMore = actions::more,
            onOpen = { open = it },
            onQuery = {
                actions.search(it)
                open = true
            }
        ) { entry ->
            open = false
            onAdd(entry, titleOf(language, entry))
        }
        OkedHint(search.hint(texts))
    }
}

/** Наименование на языке интерфейса: в заявление уходит то, что видит владелец. */
internal fun titleOf(language: Language, entry: OkedEntry): String =
    addressIn(language, entry.name, entry.nameKz)
