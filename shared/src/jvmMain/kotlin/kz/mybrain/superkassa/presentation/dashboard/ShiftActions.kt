package kz.mybrain.superkassa.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.shift.ShiftState
import kz.mybrain.superkassa.presentation.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.components.ConfirmActionDialog
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.strings.DashboardStrings
import kz.mybrain.superkassa.presentation.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.strings.blockReasonTexts
import kz.mybrain.superkassa.presentation.strings.blockReasonWords
import kz.mybrain.superkassa.presentation.strings.moneyTexts
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Glyphs
import kz.mybrain.superkassa.presentation.theme.KassaLayout
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Управление сменой.
 *
 * Закрытие смены — это Z-отчёт, и назван он так, как называет его кассир,
 * а не протокол.
 */
@Composable
internal fun ShiftActions(state: DashboardUiState, actions: DashboardActions) {
    var asking by remember { mutableStateOf(false) }
    // Вопрос стоит, пока Z-отчёт снимается: кассир видит, что нажатие
    // принято, и не жмёт второй раз.
    var confirmed by remember { mutableStateOf(false) }
    LaunchedEffect(state.busy) {
        if (confirmed && !state.busy) {
            asking = false
            confirmed = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        ShiftButtons(state, actions) { asking = true }
        ShiftNote(state)
    }
    if (asking) {
        CloseShiftAsk(state, onCancel = { asking = false }) {
            confirmed = true
            actions.closeShift()
        }
    }
}

/**
 * Главное действие экрана одно и зависит от состояния смены: закрытую
 * открывают, открытую закрывают. Остальное — тональное.
 *
 * Заблокированной кассе — и снятой с учёта в том числе — кнопок нет вовсе:
 * фискальных команд она не принимает. Неизвестное состояние смены — не повод
 * предлагать действие: «Открыть смену» над открытой сменой получало
 * SHIFT_ALREADY_OPEN.
 */
@Composable
private fun ShiftButtons(state: DashboardUiState, actions: DashboardActions, onClose: () -> Unit) {
    val texts = LocalStrings.current
    val offer = !state.blocked && state.known
    WrapRow(spacing = Spacing.snug) {
        val main = Modifier.heightIn(min = KassaLayout.mainAction)
        if (offer && state.shift == ShiftState.Open) {
            // Z-отчёт не отменяется, и до вопроса он снимался с одного нажатия.
            Button(onClick = onClose, modifier = main, enabled = state.canAct) {
                Text(texts.dashboard.closeShift)
            }
            FilledTonalButton(onClick = actions::xReport, modifier = main, enabled = state.canAct) {
                Text(texts.dashboard.xReport)
            }
        } else if (offer && state.isAdmin) {
            Button(onClick = actions::openShift, modifier = main, enabled = state.canAct) {
                Text(texts.dashboard.openShift)
            }
        }
    }
}

/**
 * Строка под кнопками: почему действия нет или что с ним не так.
 *
 * Сутки открытой смены — причина, по которой касса блокируется: до правки
 * об этом узнавали из отказа на первом чеке следующего утра.
 */
@Composable
private fun ShiftNote(state: DashboardUiState) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    val note = when {
        state.blocked -> blockReasonWords(state.kkm?.blockReasonCode, language) +
            Glyphs.SEPARATOR + blockReasonTexts(language).readingStays
        state.programming -> texts.settings.enteredProgramming
        // Смену открывает администратор: кассиру вместо кнопки, на которую
        // касса ответит отказом, сказано, кого позвать.
        state.known && state.shift != ShiftState.Open && !state.isAdmin -> texts.dashboard.openShiftAdmin
        else -> null
    }
    if (shiftTooLong(state.shiftOpenedAt, System.currentTimeMillis()) && !state.blocked) {
        Text(
            texts.dashboard.shiftTooLong,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    note?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Вопрос перед Z-отчётом: он не отменяется. */
@Composable
private fun CloseShiftAsk(state: DashboardUiState, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val texts = LocalStrings.current
    ConfirmActionDialog(
        icon = AppIcons.shiftClose,
        what = texts.dashboard.closeShiftAsk,
        explain = closeShiftExplain(state, texts.dashboard),
        action = texts.dashboard.closeShift,
        cancel = moneyTexts(LocalLanguage.current).drawer.cancel,
        busy = state.busy,
        onCancel = onCancel,
        onConfirm = onConfirm
    )
}

/**
 * Что кассир прочитает перед Z-отчётом.
 *
 * Число документов и остаток в ящике — то же, что в плитках над кнопкой:
 * решение принимают по ним. Судьба остатка названа отдельно, потому что
 * зависит от настройки кассы, а не от того, что кассир видит на экране.
 */
private fun closeShiftExplain(state: DashboardUiState, texts: DashboardStrings): String {
    val cash = if (state.kkm?.autoCashout == true) texts.closeShiftCashout else texts.closeShiftKeepsCash
    return texts.closeShiftExplain.format(
        state.documents.size.toString(),
        Money.formatTiyn(state.cashInDrawer)
    ) + " " + cash
}
