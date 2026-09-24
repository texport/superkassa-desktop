package kz.mybrain.superkassa.presentation.journal.shifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.followSeat
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.journal.PageOutcome
import kz.mybrain.superkassa.presentation.journal.outcome
import kz.mybrain.superkassa.presentation.strings.common.stringsOf

/**
 * Прошлые смены и их документы.
 *
 * Z-отчёт закрытой позавчера смены иначе не найти: журнал за срок
 * показывает документы, а не смены. Смены и документы смены читаются
 * страницами и дочитываются по требованию.
 */
class ShiftsViewModel(private val cases: ShiftsCases, private val talk: Talk) : ViewModel(), ShiftsActions {
    private val screen = MutableStateFlow(ShiftsUiState())
    private val reading = latest()

    val state: StateFlow<ShiftsUiState> = screen.asStateFlow()

    init {
        // Сел другой кассир или ушёл: прочитанные смены принадлежали прежнему.
        followSeat(cases.observe(), seated = { reload() })
        viewModelScope.launch {
            cases.readDocumentTypes()?.let { types -> screen.update { it.copy(documentTypes = types) } }
        }
    }

    /** Перечитывает смены с начала; за кассой никого нет — спрашивать некого. */
    override fun reload() {
        val seated = cases.observe().value.signedIn
        screen.update { ShiftsUiState(loading = seated, documentTypes = it.documentTypes) }
        if (seated) reading.restart { readShifts() } else reading.cancel()
    }

    /** Дочитывает следующую страницу смен. */
    override fun more() {
        if (screen.value.loading) return
        reading.restart { readShifts() }
    }

    override fun open(shift: ShiftResponse?) {
        reading.cancel()
        // Ожидание ставится вместе с открытием: иначе до первого кадра
        // чтения экран успевал объяснить пустоту смены, которую ещё никто
        // не спрашивал.
        screen.update {
            it.copy(opened = shift, documents = emptyList(), documentsPage = PageOutcome.unread)
                .copy(opening = shift != null)
        }
        if (shift != null) reading.restart { readDocuments(shift) }
    }

    /** Дочитывает документы открытой смены; после неудачи — читает их заново. */
    override fun moreDocuments() {
        val shift = screen.value.opened ?: return
        if (screen.value.opening) return
        screen.update { it.copy(opening = true) }
        reading.restart { readDocuments(shift) }
    }

    private suspend fun readShifts() {
        screen.update { it.copy(loading = true) }
        val page = cases.readShifts(screen.value.shifts).shown(what(), "read shifts", talk)
        screen.update { now ->
            val (shifts, outcome) = page.outcome(now.shifts, now.page)
            now.copy(shifts = shifts, page = outcome, loading = false)
        }
    }

    private suspend fun readDocuments(shift: ShiftResponse) {
        val answer = cases.readShiftDocuments(shift.id, screen.value.documents)
        val page = answer.shown(what(), "read shift documents", talk)
        screen.update { now ->
            val (documents, outcome) = page.outcome(now.documents, now.documentsPage)
            now.copy(documents = documents, documentsPage = outcome, opening = false)
        }
    }

    private fun what(): String = stringsOf(talk.language()).sections.history
}
