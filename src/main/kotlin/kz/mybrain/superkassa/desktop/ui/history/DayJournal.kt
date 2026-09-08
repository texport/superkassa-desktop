package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.PAGE
import kz.mybrain.superkassa.desktop.server.documents
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Журнал за выбранный день.
 *
 * День перелистывается кнопками, а не вводится: кассир ищет «вчера» или
 * «позавчера», и заставлять его набирать дату ради этого незачем. Журнал
 * перечитывается сам при смене дня — экран, который до нажатия кнопки
 * показывает пустоту, ничему не учит.
 */
@Composable
fun DayJournal(session: Session) {
    val texts = LocalStrings.current
    val journal = journalTexts(session.language).history
    var day by remember { mutableStateOf(LocalDate.now()) }
    var chosenType by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loaded = remember { mutableStateListOf<Document>() }
    // Пришла ли последняя страница целиком: если да, за ней есть ещё.
    // Прежде журнал брал первые двести документов и молчал об остальных —
    // за оживлённый день он показывал часть дня, не сообщая, что это часть.
    var more by remember { mutableStateOf(false) }

    LaunchedEffect(day, session.selected?.kkmId) {
        loading = true
        loaded.clear()
        more = loadDay(session, texts.sections.history, day, loaded)
        loading = false
    }

    val types = documentTypesIn(loaded, session.documentTypeOrder())
    // Отбор, переживший смену дня, снимается сам: чек такого типа вчера мог
    // и не пробиваться, а пустой список без объяснения выглядит утратой.
    val type = chosenType?.takeIf { it in types }
    val shown = filterByType(loaded, type)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        DayBar(journal, day, loading) { day = it }
        TypeFilter(session, journal, types, type) { chosenType = it }
        when {
            loading -> JournalLoading(Modifier.weight(1f))
            loaded.isEmpty() -> JournalEmpty(
                icon = JournalIcons.noDocuments,
                line = journal.emptyDay,
                hint = journal.emptyDayHint,
                modifier = Modifier.weight(1f)
            )
            shown.isEmpty() -> JournalEmpty(
                icon = JournalIcons.noDocuments,
                line = journal.emptyForType,
                hint = journal.emptyForTypeHint,
                modifier = Modifier.weight(1f)
            )
            else -> {
                Text(
                    text = "${journal.shown}: ${shown.size} / ${loaded.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DocumentJournalHeader(journal)
                ScrollableList(modifier = Modifier.weight(1f)) {
                    itemsIndexed(shown) { at, document ->
                        DocumentJournalRow(
                            session = session,
                            document = document,
                            striped = at % 2 == 1,
                            onPreview = { session.printDesk.preview(document) },
                            onPrint = { session.printDesk.print(document) }
                        )
                    }
                }
                MoreRow(more, loading, journal.showMore, journal.allShown) {
                    scope.launch {
                        loading = true
                        more = loadDay(session, texts.sections.history, day, loaded)
                        loading = false
                    }
                }
            }
        }
    }
}

/** Перелистывание дня. Вперёд дальше сегодняшнего идти некуда. */
@Composable
private fun DayBar(
    journal: HistoryJournalTexts,
    day: LocalDate,
    loading: Boolean,
    onDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = journal.day,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(enabled = !loading, onClick = { onDay(day.minusDays(1)) }) {
            Icon(JournalIcons.earlierDay, contentDescription = journal.earlierDay)
        }
        Text(DAY.format(day), style = MaterialTheme.typography.titleMedium)
        IconButton(enabled = !loading && day < today, onClick = { onDay(day.plusDays(1)) }) {
            Icon(JournalIcons.laterDay, contentDescription = journal.laterDay)
        }
        TextButton(enabled = !loading && day != today, onClick = { onDay(today) }) {
            Icon(JournalIcons.today, contentDescription = null)
            Text(journal.today, modifier = Modifier.padding(start = Spacing.tight))
        }
    }
}

/**
 * Отбор по типу документа.
 *
 * Один ряд с прокруткой: типов у кассы бывает десяток, и перенос их
 * на вторую строку сдвигал бы таблицу под кассиром при каждом нажатии.
 * Названия типов — из справочника узла.
 */
@Composable
private fun TypeFilter(
    session: Session,
    journal: HistoryJournalTexts,
    types: List<String>,
    chosen: String?,
    onChoose: (String?) -> Unit
) {
    if (types.isEmpty()) return
    val texts = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = chosen == null,
            onClick = { onChoose(null) },
            label = { Text(journal.allTypes) }
        )
        types.forEach { code ->
            FilterChip(
                selected = chosen == code,
                onClick = { onChoose(code) },
                label = { Text(documentTypeTitle(session, texts, code)) }
            )
        }
    }
}

/** Порядок типов в справочнике узла: отбор идёт в нём, а не по алфавиту. */
private fun Session.documentTypeOrder(): List<String> =
    dictionaries[Dictionary.DocumentTypes].orEmpty().map { it.code }

/**
 * Дочитывает день с того места, где остановились.
 *
 * @return есть ли за пришедшей страницей ещё документы.
 */
private suspend fun loadDay(
    session: Session,
    what: String,
    day: LocalDate,
    into: MutableList<Document>
): Boolean {
    val kkm = session.selected ?: return false
    val range = dayRange(day)
    val loaded = session.guard(what) {
        session.client.documents(kkm.kkmId, range.fromMillis, range.toMillis, session.pin, into.size)
    } ?: return false
    into.addAll(loaded)
    return loaded.size == PAGE
}

private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
