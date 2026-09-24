package kz.mybrain.superkassa.presentation.journal.documents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.common.document.JournalEmpty
import kz.mybrain.superkassa.presentation.common.document.JournalEntry
import kz.mybrain.superkassa.presentation.common.document.JournalType
import kz.mybrain.superkassa.presentation.common.document.JournalView
import kz.mybrain.superkassa.presentation.common.document.presentIn
import kz.mybrain.superkassa.presentation.common.period.JournalPeriodBar
import kz.mybrain.superkassa.presentation.print.preview.PrintActions
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Журнал документов кассы за выбранный срок.
 *
 * Срок перелистывается стрелками, а не вводится: кассир ищет «вчера»
 * или «прошлую неделю», и заставлять его набирать даты ради этого незачем.
 *
 * Поиск, отбор, порядок строк и таблицу рисует общий [JournalView]: тем же
 * журналом показаны документы кассы в кабинете, и разойтись им нельзя.
 * Здесь остаётся только источник — эта касса.
 *
 * Нажатие на чек открывает его доставку покупателю — по каналам и с
 * повтором туда, куда чек не дошёл.
 */
@Composable
fun JournalScreen(state: JournalUiState, actions: JournalActions, print: PrintActions) {
    val journal = textsOf(LocalLanguage.current).journal.history
    val (entries, types) = rowsOf(state)
    val printing = print.of(state.documents)
    Column(modifier = Modifier.fillMaxSize()) {
        JournalView(
            journal = journal,
            entries = entries,
            types = types,
            // Отбор, переживший смену срока, снимается сам: чек такого вида
            // вчера мог и не пробиваться, а пустой список без объяснения
            // выглядит утратой документов.
            query = state.query.presentIn(entries),
            loading = state.loading,
            more = state.page.more,
            empty = JournalEmpty(journal.emptyDay, journal.emptyDayHint),
            unread = !state.page.read,
            onQuery = actions::filter,
            onMore = actions::more,
            onRetry = actions::more,
            onOpen = { entry -> actions.open(entry.key) },
            onPreview = printing.preview,
            onPrint = printing.print,
            // Срок стоит над поиском и прокручивается вместе с отбором:
            // в низком окне они вместе не должны вытеснять строки.
            head = { JournalPeriodBar(journal, state.period, state.loading, actions::choose) }
        )
    }
}

/**
 * Строки журнала и виды документов для отбора.
 *
 * Пересчитываются при смене прочитанного или языка, а не на каждый кадр:
 * за месяц строк тысячи.
 */
@Composable
private fun rowsOf(state: JournalUiState): Pair<List<JournalEntry>, List<JournalType>> {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    return remember(state.documents, state.documentTypes, language) {
        journalEntriesOf(texts, language, state.documentTypes, state.documents) to
            journalTypesOf(texts, language, state.documentTypes, state.documents)
    }
}

/** Показ и печать строки журнала: строка находит свой документ по ключу. */
internal class EntryPrint(val preview: (JournalEntry) -> Unit, val print: (JournalEntry) -> Unit)

/** Печать строк из прочитанных [documents]. */
internal fun PrintActions.of(documents: List<FiscalDocumentResponse>): EntryPrint {
    fun documentOf(entry: JournalEntry) = documents.firstOrNull { it.id == entry.key }
    return EntryPrint(
        preview = { entry -> documentOf(entry)?.let(::preview) },
        print = { entry -> documentOf(entry)?.let { print(it.id) } }
    )
}
