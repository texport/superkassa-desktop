package kz.mybrain.superkassa.presentation.common.document

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.field.SearchField
import kz.mybrain.superkassa.designsystem.picker.SieveChip
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.journal.HistoryJournalTexts

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
internal fun JournalToolbar(journal: HistoryJournalTexts, query: JournalQuery, onQuery: (JournalQuery) -> Unit) {
    // Поле тянется до конца строки, пока ему хватает наименьшей ширины;
    // не хватает — порядок уходит на строку ниже, а не сжимает поле
    // до слова столбиком.
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.fieldGap) {
        SearchField(
            value = query.search,
            label = journal.search,
            onChange = { onQuery(query.copy(search = it)) },
            modifier = Modifier.weight(1f).widthIn(min = Sizes.fieldSearch),
            hint = journal.searchHint,
            clearLabel = journal.clearSearch
        )
        // Плашки порядка того же роста, что поле поиска, и стоят с ним
        // вровень. В узком окне они переносятся по одной, а не рвут
        // надпись плашки.
        WrapRow(spacing = Spacing.buttonGap) {
            SortChips(journal, query, onQuery)
        }
    }
}

/**
 * Порядок строк: по чему сортировать и в какую сторону.
 *
 * Поле выбирается плашками, сторона — значком рядом: два набора плашек
 * под одно решение читались как шесть не связанных между собой отборов.
 * Подписи «Порядок» перед плашками нет: «По времени», «По сумме» говорят
 * сами за себя.
 */
@Composable
private fun SortChips(journal: HistoryJournalTexts, query: JournalQuery, onQuery: (JournalQuery) -> Unit) {
    JournalSort.entries.forEach { sort ->
        SieveChip(
            selected = query.sort == sort,
            label = sort.title(journal),
            onClick = { onQuery(query.copy(sort = sort)) }
        )
    }
    IconButton(onClick = { onQuery(query.copy(descending = !query.descending)) }) {
        Icon(
            imageVector = if (query.descending) AppIcons.descending else AppIcons.ascending,
            contentDescription = if (query.descending) journal.descending else journal.ascending
        )
    }
}
