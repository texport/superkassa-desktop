package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.SearchField
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.fieldLabelReserve

/**
 * Поиск и порядок строк журнала.
 *
 * Одна строка над таблицей: слева — что ищем, справа — как разложить
 * найденное. Поиск и порядок стоят рядом потому, что делают одно дело —
 * помогают найти документ, — и разнесённые по разным углам экрана они
 * читались как два не связанных между собой средства.
 *
 * Набор одинаков у журнала кассы и у документов кассы в кабинете: искать
 * чек владелец и кассир будут одинаково.
 */
@Composable
fun JournalToolbar(journal: HistoryJournalTexts, query: JournalQuery, onQuery: (JournalQuery) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.Top
    ) {
        SearchField(
            value = query.search,
            label = journal.search,
            onChange = { onQuery(query.copy(search = it)) },
            modifier = Modifier.weight(1f),
            hint = journal.searchHint,
            clearLabel = journal.clearSearch
        )
        // Плашки порядка стоят на высоте рамки поля, а не по центру всей
        // его высоты: поле держит над рамкой место под поднятую подпись.
        Row(
            modifier = Modifier.padding(top = fieldLabelReserve()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortChips(journal, query, onQuery)
        }
    }
}

/**
 * Порядок строк: по чему сортировать и в какую сторону.
 *
 * Поле выбирается плашками, сторона — значком рядом: два набора плашек
 * под одно решение читались как шесть не связанных между собой отборов.
 */
@Composable
private fun SortChips(journal: HistoryJournalTexts, query: JournalQuery, onQuery: (JournalQuery) -> Unit) {
    Text(
        text = journal.sort,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    JournalSort.entries.forEach { sort ->
        FilterChip(
            selected = query.sort == sort,
            onClick = { onQuery(query.copy(sort = sort)) },
            label = { Text(sort.title(journal)) }
        )
    }
    IconButton(onClick = { onQuery(query.copy(descending = !query.descending)) }) {
        Icon(
            imageVector = if (query.descending) AppIcons.descending else AppIcons.ascending,
            contentDescription = if (query.descending) journal.descending else journal.ascending
        )
    }
}
