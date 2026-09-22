package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.PrintFileName
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.format.DateTimeFormatter

/**
 * Журнал документов кассы за выбранный срок.
 *
 * Срок перелистывается стрелками, а не вводится: кассир ищет «вчера»
 * или «прошлую неделю», и заставлять его набирать даты ради этого незачем.
 * Журнал перечитывается сам при смене срока — экран, который до нажатия
 * кнопки показывает пустоту, ничему не учит.
 *
 * Поиск, отбор, порядок строк и таблицу рисует общий [JournalView]: тем же
 * журналом показаны документы кассы в кабинете, и разойтись им нельзя.
 * Здесь остаётся только источник — узел этой кассы.
 */
@Composable
fun DayJournal(session: Session) {
    val texts = LocalStrings.current
    val journal = journalTexts(session.language).history
    var period by remember { mutableStateOf(JournalPeriod.of(JournalSpan.Day)) }
    var query by remember { mutableStateOf(JournalQuery()) }
    var loading by remember { mutableStateOf(false) }
    val loaded = remember { mutableStateListOf<Document>() }
    // Чем кончилось чтение: за полной страницей есть ещё, а отказ узла
    // нельзя показывать пустым сроком. Прежде журнал брал первые двести
    // документов и молчал об остальных — за оживлённый день он показывал
    // часть дня, не сообщая, что это часть.
    var load by remember { mutableStateOf(JournalLoad.Whole) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(period, session.selected?.kkmId) {
        loading = true
        loaded.clear()
        load = loadPeriod(session, texts.sections.history, period, loaded)
        loading = false
    }

    val entries = journalEntriesOf(session, texts, loaded)
    val types = documentTypesIn(loaded, session.documentTypeOrder())
        .map { code -> JournalType(code, documentTypeTitle(session, texts, code)) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        JournalPeriodBar(journal, period, loading) { period = it }
        JournalView(
            journal = journal,
            entries = entries,
            types = types,
            // Отбор, переживший смену срока, снимается сам: чек такого вида
            // вчера мог и не пробиваться, а пустой список без объяснения
            // выглядит утратой документов.
            query = query.presentIn(entries),
            loading = loading,
            more = load.more,
            empty = JournalEmpty(journal.emptyDay, journal.emptyDayHint),
            unreadable = if (load.failed) JournalEmpty(journal.unreadable, texts.common.unreadableHint) else null,
            onQuery = { query = it },
            onMore = {
                scope.launch {
                    loading = true
                    load = loadPeriod(session, texts.sections.history, period, loaded)
                    loading = false
                }
            },
            onPreview = { entry ->
                session.printDesk.previewDocument(
                    entry.key,
                    PrintFileName.of(entry.typeCode, entry.number, entry.shiftNo)
                )
            },
            onPrint = { entry -> loaded.firstOrNull { it.id == entry.key }?.let(session.printDesk::print) }
        )
    }
}

/** Порядок типов в справочнике узла: отбор идёт в нём, а не по алфавиту. */
private fun Session.documentTypeOrder(): List<String> =
    dictionaries[Dictionary.DocumentTypes].orEmpty().map { it.code }

internal val JOURNAL_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
