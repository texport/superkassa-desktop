package kz.mybrain.superkassa.presentation.print.target

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.presentation.common.model.follow

/**
 * Принтер кассы и вид файла печатной формы.
 *
 * Принтер выбирается один раз и держится за кассой: за одним компьютером
 * их бывает две, и чековая лента у каждой своя. Выбор действует сразу —
 * отдельной кнопки у него нет, и говорить кассиру не о чем.
 */
internal class PrintTargetViewModel(private val cases: PrintTargetCases) : ViewModel(), PrintTargetActions {
    private val screen = MutableStateFlow(PrintTargetUiState())

    val state: StateFlow<PrintTargetUiState> = screen.asStateFlow()

    init {
        follow(cases.observe().map { it.kkm?.kkmId }.distinctUntilChanged(), ::onKkm)
    }

    override fun choosePrinter(name: String?) {
        val kkmId = screen.value.kkmId ?: return
        cases.printer(kkmId, name)
        screen.update { it.copy(printer = name) }
    }

    override fun chooseCopies(copies: Int) {
        val saved = cases.copies(copies)
        screen.update { it.copy(copies = saved) }
    }

    override fun chooseKind(kind: PrintKind) {
        cases.kind(kind)
        screen.update { it.copy(kind = kind) }
    }

    /** Касса сменилась: её принтер свой, а принтеры машины могли подключить. */
    private suspend fun onKkm(kkmId: String?) {
        val target = cases.read(kkmId)
        screen.update {
            PrintTargetUiState(
                kkmId,
                target.printers,
                printersRead = true,
                target.printer,
                target.copies,
                target.kind,
                target.route
            )
        }
    }
}
