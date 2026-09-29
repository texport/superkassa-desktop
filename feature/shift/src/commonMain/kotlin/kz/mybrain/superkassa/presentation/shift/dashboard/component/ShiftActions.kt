package kz.mybrain.superkassa.presentation.shift.dashboard.component

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.dialog.ConfirmActionDialog
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.KassaLayout
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardActions
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardUiState
import kz.mybrain.superkassa.strings.api.common.DashboardTexts
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Управление сменой.
 *
 * Закрытие смены — это Z-отчёт, и назван он так, как называет его кассир,
 * а не протокол.
 */
@Composable
internal fun ShiftActions(state: DashboardUiState, actions: DashboardActions) {
    // Вопрос о Z-отчёте переживает поворот экрана: на Android поворот
    // пересоздаёт окно, и открытый вопрос пропадал из-под пальца.
    var asking by rememberSaveable { mutableStateOf(false) }
    // Вопрос стоит, пока Z-отчёт снимается: кассир видит, что нажатие
    // принято, и не жмёт второй раз.
    var confirmed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.busy) {
        if (confirmed && !state.busy) {
            asking = false
            confirmed = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
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
 * X-отчёт в смене без чеков и внесений выключен: БФД такую смену ещё
 * не открыл, и отчёт вернулся бы отказом.
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
    WrapRow(spacing = Spacing.fieldGap) {
        val main = Modifier.heightIn(min = KassaLayout.mainAction)
        if (offer && state.shift == ShiftState.Open) {
            // Z-отчёт не отменяется, и до вопроса он снимался с одного нажатия.
            Button(onClick = onClose, modifier = main, enabled = state.canAct) {
                Text(texts.dashboard.closeShift)
            }
            FilledTonalButton(onClick = actions::xReport, modifier = main, enabled = state.canTakeXReport) {
                Text(texts.dashboard.xReport)
            }
        } else if (offer) {
            ClosedShiftButtons(state, actions, main)
        }
    }
}

/**
 * Строка под кнопками: почему действия нет или что с ним не так.
 *
 * Предел суток открытой смены называет сама касса — тем же правилом
 * и по тем же часам, по которым она откажет в чеке. До правки экран
 * считал сутки сам от открытия смены, расходился с кассой и писал
 * о пределе даже над закрытой сменой.
 */
@Composable
private fun ShiftNote(state: DashboardUiState) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    val note = when {
        state.blocked -> textsOf(language).kassa.blockReason.words(state.kkm?.blockReasonCode) +
            Glyphs.SEPARATOR + textsOf(language).kassa.blockReason.readingStays
        state.programming -> texts.settingsScreen.enteredProgramming
        // Смену открывает администратор: кассиру вместо кнопки, на которую
        // касса ответит отказом, сказано, кого позвать.
        state.known && state.shift != ShiftState.Open && !state.isAdmin -> texts.dashboard.openShiftAdmin
        else -> null
    }
    DayLimit(state)
    note?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Предел суток смены: пройден — ошибкой, впереди — когда закрыть.
 *
 * Только у открытой смены и незаблокированной кассы: над закрытой сменой
 * «закройте её» читается как поломка.
 */
@Composable
private fun DayLimit(state: DashboardUiState) {
    if (state.blocked || state.shift != ShiftState.Open) return
    val texts = LocalStrings.current.dashboard
    val limit = state.dayLimitAt
    when {
        state.dayLimitExceeded -> Text(
            texts.shiftTooLong,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        limit != null -> Text(
            texts.shiftDayLimit.fill(Dates.shortMoment(limit)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        cancel = textsOf(LocalLanguage.current).kassa.money.drawer.cancel,
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
private fun closeShiftExplain(state: DashboardUiState, texts: DashboardTexts): String {
    val cash = if (state.kkm?.autoCashout == true) texts.closeShiftCashout else texts.closeShiftKeepsCash
    return texts.closeShiftExplain.fill(
        state.documents.size.toString(),
        Money.formatTiyn(state.cashInDrawer)
    ) + " " + cash
}
