package kz.mybrain.superkassa.presentation.journal.shifts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.designsystem.list.MoreRow
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.shift.model.zReportId
import kz.mybrain.superkassa.presentation.common.document.color
import kz.mybrain.superkassa.presentation.common.period.text
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.common.print.PrintFileName
import kz.mybrain.superkassa.presentation.journal.PageOutcome
import kz.mybrain.superkassa.strings.api.journal.ShiftJournalTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Прошлые смены и их документы.
 *
 * Смены касса отдаёт отдельно от журнала за срок: без них Z-отчёт
 * закрытой позавчера смены нельзя было ни найти, ни напечатать.
 */
@Composable
fun ShiftsScreen(state: ShiftsUiState, actions: ShiftsActions, print: PrintActions) {
    val journal = textsOf(LocalLanguage.current).journal.shifts
    val opened = state.opened
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        Text(
            text = journal.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (opened == null) {
            ShiftList(journal, state, actions) { shift ->
                print.preview(shift.zReportId, PrintFileName.zReport(shift.shiftNo))
            }
        } else {
            ShiftDocuments(journal, state, actions, print)
        }
    }
}

@Composable
private fun ColumnScope.ShiftList(
    journal: ShiftJournalTexts,
    state: ShiftsUiState,
    actions: ShiftsActions,
    onZReport: (ShiftResponse) -> Unit
) {
    val slot = shiftsState(journal, state.shifts.size, state.loading, state.page, actions::reload)
    ScreenSlot(slot, Modifier.weight(1f)) {
        ShiftTable(journal, state.shifts, Modifier.weight(1f), actions::open, onZReport)
        // Под списком видно, кончились ли смены: молчание внизу не отличает
        // «всё» от «оборвалось на двухсотой».
        MoreRow(state.page.more, state.loading, journal.showMore, journal.allShown, onMore = actions::more)
    }
}

/**
 * Что стоит на месте списка смен.
 *
 * «Смен нет» — утверждение о кассе, и говорить его можно только вслед
 * за ответом кассы. Касса, которая отказала или промолчала, о сменах ничего
 * не сказала: кассир, пришедший за Z-отчётом позавчерашней смены, читал
 * его молчание как утрату смены.
 */
internal fun shiftsState(
    journal: ShiftJournalTexts,
    shifts: Int,
    loading: Boolean,
    page: PageOutcome,
    onRetry: () -> Unit
): ScreenState = when {
    // Ожидание встаёт на место списка только до первого ответа: прочитанные
    // смены остаются на экране, пока читается следующая страница.
    loading && shifts == 0 -> ScreenState.Working
    // Прочитанные прежде смены остаются на месте: неудача дочитывания
    // не повод убирать с экрана то, что кассир уже видит.
    !page.read && shifts == 0 -> ScreenState.Trouble(journal.unread, journal.unreadHint, onRetry)
    shifts == 0 -> ScreenState.Empty(AppIcons.noDocuments, journal.none, journal.noneHint)
    else -> ScreenState.Ready
}
