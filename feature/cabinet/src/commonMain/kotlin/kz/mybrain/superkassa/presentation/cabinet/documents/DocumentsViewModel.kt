package kz.mybrain.superkassa.presentation.cabinet.documents

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.packet
import kz.mybrain.superkassa.presentation.cabinet.CabinetReply
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.cabinetMessage
import kz.mybrain.superkassa.presentation.cabinet.problem
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.document.JournalEntry
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.print.PrintFileName

/**
 * Документы кассы по данным ОФД: то, что принял сервер приёма данных.
 *
 * Модель живёт, пока открыто окно: владелец уходит к карточке кассы
 * и возвращается к тем же виду, сроку и прочитанным страницам. Другая
 * касса — чистый лист.
 */
internal class DocumentsViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(DocumentsUiState())
    private val cases = cabinet.useCases
    private var reading: Job? = null

    val state: StateFlow<DocumentsUiState> = screen.asStateFlow()

    /** Документы кассы открыты: та же касса — прочитанное остаётся. */
    fun show(register: CabinetRegister) {
        if (screen.value.registerId == register.id) return
        restart(DocumentsUiState(registerId = register.id))
    }

    fun kind(kind: DocumentKind) = restart(screen.value.copy(kind = kind))

    fun period(period: JournalPeriod) = restart(screen.value.copy(period = period))

    fun query(query: JournalQuery) = screen.update { it.copy(query = query) }

    /** Следующая страница: во время чтения вторая не начинается. */
    fun more() {
        if (screen.value.loading) return
        reading = viewModelScope.launch { readPage() }
    }

    /** Раскрывает документ строки. */
    fun open(key: String) {
        val now = screen.value
        val id = now.registerId ?: return
        val target = now.targetOf(key) ?: return
        screen.update { it.copy(opening = true) }
        viewModelScope.launch {
            val opened = cabinet.work.run("open cabinet document") { cases.openDocument(id, now.kind, target) }.value
            screen.update { it.copy(opened = opened, opening = false) }
        }
    }

    fun close() = screen.update { it.copy(opened = null, opening = false) }

    /**
     * Печатная форма документа строки.
     *
     * Документ берётся тем же обращением, которым он раскрывается на экране:
     * список отдаёт только сводку строки, а пакет лежит в самом документе.
     * Отказ кабинета уже показан — второго сообщения о том же не нужно.
     * Рисует форму окно печати — по пакету протокола, как и любой документ
     * кабинета: [onForm] получает пакет, имя документа и имя файла.
     */
    fun form(entry: JournalEntry, onForm: (packet: String, name: String, file: String?) -> Unit) {
        val now = screen.value
        val id = now.registerId ?: return
        val target = now.targetOf(entry.key) ?: return
        viewModelScope.launch {
            val reply = cabinet.work.run("open cabinet document") { cases.openDocument(id, now.kind, target) }
            if (reply is CabinetReply.Failed) return@launch
            val packet = reply.value?.packet
            if (packet == null) {
                cabinet.talk.say("open cabinet document", Message.Refusal(cabinet.texts.documentDataMissing, NO_PACKET))
                return@launch
            }
            onForm(packet, entry.key, PrintFileName.of(entry.typeCode, entry.number, entry.shiftNo))
        }
    }

    /**
     * Начинает с чистого листа: сменился вид, срок или касса. Чтение
     * прошлого списка отменяется вместе со своим ожиданием: оставленный
     * признак чтения запер бы новый список навсегда.
     */
    private fun restart(fresh: DocumentsUiState) {
        reading?.cancel()
        screen.value = DocumentsUiState(fresh.registerId, fresh.kind, fresh.period, fresh.query)
        val id = fresh.registerId ?: return
        reading = viewModelScope.launch {
            val overview = cabinet.work.run("read documents overview") { cases.readOverview(id) }
            screen.update { it.copy(overview = overview.value) }
            readPage()
        }
    }

    private suspend fun readPage() {
        val now = screen.value
        val id = now.registerId ?: return
        screen.update { it.copy(loading = true) }
        val period = cabinetPeriodOf(now.period)
        val texts = cabinet.texts
        val reply = try {
            cabinet.work.run("read cabinet documents") {
                sliceOf(cases.readDocuments(id, now.kind, now.page, period), texts)
            }
        } finally {
            screen.update { it.copy(loading = false) }
        }
        val slice = reply.value
        if (slice == null) {
            val words = reply.problem?.let { cabinetMessage(it, texts).words() } ?: texts.unreachable
            screen.update { it.copy(trouble = words) }
            return
        }
        screen.update { it.copy(rows = it.rows + slice.rows, total = slice.total, page = it.page + 1, trouble = null) }
    }
}

/** Код сообщения о документе без пакета: у кабинета нет того, по чему его нарисовать. */
private const val NO_PACKET = "DOCUMENT_DATA_MISSING"

/** Модель документов кассы окна. */
@Composable
internal fun documentsViewModel(cabinet: CabinetViewModel): DocumentsViewModel =
    viewModel { DocumentsViewModel(cabinet) }
