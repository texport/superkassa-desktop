package kz.mybrain.superkassa.presentation.shift.dashboard.component

import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardActions
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardUiState
import kz.mybrain.superkassa.strings.api.fill

/**
 * Закрытая смена: открыть новую — главное действие администратора;
 * Z-отчёт только что закрытой — тональное, и кассиру тоже. Сразу после
 * закрытия отчёт иначе приходилось искать в журнале по сменам.
 */
@Composable
internal fun ClosedShiftButtons(state: DashboardUiState, actions: DashboardActions, main: Modifier) {
    val texts = LocalStrings.current
    if (state.isAdmin) {
        Button(onClick = actions::openShift, modifier = main, enabled = state.canAct) {
            Text(texts.dashboard.openShift)
        }
    }
    state.zReportShift?.let { shift ->
        FilledTonalButton(onClick = { actions.zReport(shift) }, modifier = main, enabled = !state.busy) {
            Text(texts.dashboard.lastZReport.fill(shift.shiftNo))
        }
    }
}
