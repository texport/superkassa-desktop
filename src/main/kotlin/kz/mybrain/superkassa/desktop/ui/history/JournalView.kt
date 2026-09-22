package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Журнал документов целиком: поиск, отбор, таблица и подгрузка.
 *
 * Тело обоих экранов — журнала кассы и документов кассы в кабинете.
 * Различается у них только источник строк: узел рядом отдаёт документы
 * своей кассы, кабинет — то, что принял сервер приёма данных. Всё
 * остальное — как искать, чем отбирать, в каком порядке показывать,
 * где кнопки просмотра и печати — здесь и в одном виде.
 *
 * Отбор и порядок держит вызывающий: у кабинета вид документа выбирает,
 * какой список читать у сервера, а у кассы — отбирает уже прочитанное.
 *
 * @param entries прочитанные строки как есть; отбор накладывается здесь.
 * @param types виды документов для отбора.
 * @param empty слова источника о том, что у него пусто.
 * @param unreadable слова о том, что прочитать не удалось; `null` — чтение
 *   прошло. Пустота после отказа — не пустота, и повторить её предлагается
 *   тем же чтением, каким берут следующую страницу.
 * @param onOpen что показать по нажатию на строку; `null` — строка
 *   не нажимается.
 * @param onPreview показ печатной формы; `null` — формы нет.
 * @param onPrint отправка на принтер; `null` — печатать нечем.
 */
@Composable
fun ColumnScope.JournalView(
    journal: HistoryJournalTexts,
    entries: List<JournalEntry>,
    types: List<JournalType>,
    query: JournalQuery,
    loading: Boolean,
    more: Boolean,
    empty: JournalEmpty,
    unreadable: JournalEmpty? = null,
    onQuery: (JournalQuery) -> Unit,
    onMore: () -> Unit,
    onOpen: ((JournalEntry) -> Unit)? = null,
    onPreview: ((JournalEntry) -> Unit)? = null,
    onPrint: ((JournalEntry) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        JournalToolbar(journal, query, onQuery)
        JournalFilters(
            journal = journal,
            types = types,
            deliveries = deliveriesIn(entries),
            shifts = shiftsIn(entries),
            query = query,
            onQuery = onQuery
        )
    }
    // Отбор пересчитывается при смене строк или отбора, а не на каждый
    // набранный знак: за месяц строк тысячи, и сортировка их на каждое
    // нажатие клавиши подтормаживала бы ввод.
    val shown = remember(entries, query) { entries.select(query) }
    val state = when {
        loading && entries.isEmpty() -> ScreenState.Working
        unreadable != null && entries.isEmpty() ->
            ScreenState.Trouble(unreadable.title, unreadable.hint, onRetry = onMore)

        entries.isEmpty() -> ScreenState.Empty(AppIcons.noDocuments, empty.title, empty.hint)
        shown.isEmpty() -> ScreenState.Empty(
            icon = AppIcons.noDocuments,
            title = journal.emptyForFilter,
            hint = journal.emptyForFilterHint
        )

        else -> ScreenState.Ready
    }
    ScreenSlot(state, Modifier.weight(1f)) {
        JournalRows(journal, entries, shown, loading, more, onMore, onOpen, onPreview, onPrint)
    }
}

/** Сколько показано из скольких, таблица и низ списка. */
@Composable
private fun ColumnScope.JournalRows(
    journal: HistoryJournalTexts,
    entries: List<JournalEntry>,
    shown: List<JournalEntry>,
    loading: Boolean,
    more: Boolean,
    onMore: () -> Unit,
    onOpen: ((JournalEntry) -> Unit)?,
    onPreview: ((JournalEntry) -> Unit)?,
    onPrint: ((JournalEntry) -> Unit)?
) {
    Text(
        text = "${journal.shown}: ${shown.size} / ${entries.size}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    JournalHeader(journal)
    ScrollableList(modifier = Modifier.weight(1f)) {
        itemsIndexed(shown, key = { _, entry -> entry.key }) { at, entry ->
            JournalRow(
                entry = entry,
                striped = stripedAt(at),
                onOpen = onOpen?.let { open -> { open(entry) } },
                onPreview = onPreview?.let { preview -> { preview(entry) } },
                onPrint = onPrint?.let { print -> { print(entry) } }
            )
        }
    }
    MoreRow(more, loading, journal.showMore, journal.allShown, onMore = onMore)
}
