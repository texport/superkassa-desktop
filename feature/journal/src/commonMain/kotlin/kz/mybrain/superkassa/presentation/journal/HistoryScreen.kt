package kz.mybrain.superkassa.presentation.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.picker.ChoiceSegments
import kz.mybrain.superkassa.designsystem.section.ScreenTitle
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.journal.documents.HistoryView
import kz.mybrain.superkassa.presentation.journal.documents.JournalActions
import kz.mybrain.superkassa.presentation.journal.documents.JournalScreen
import kz.mybrain.superkassa.presentation.journal.documents.JournalUiState
import kz.mybrain.superkassa.presentation.journal.documents.JournalViewModel
import kz.mybrain.superkassa.presentation.journal.documents.component.ReceiptDeliveryDialog
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsActions
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsScreen
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsUiState
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsViewModel
import kz.mybrain.superkassa.strings.api.textsOf

/** Журнал документов: состояние — из моделей, действия — им же. */
@Composable
fun HistoryScreen(journal: JournalViewModel, shifts: ShiftsViewModel, print: PrintActions) {
    val day by journal.state.collectAsScreenState()
    val past by shifts.state.collectAsScreenState()
    // Журнал перечитывается, как раздел открыт: чек, пробитый минуту
    // назад в продаже, обязан в нём уже стоять.
    LaunchedEffect(day.view) {
        when (day.view) {
            HistoryView.Day -> journal.choose(day.period)
            HistoryView.Shifts -> shifts.reload()
        }
    }
    HistoryContent(HistoryParts(day, journal, past, shifts, print))
}

/**
 * Всё, из чего собран журнал: два взгляда, их действия и печать.
 *
 * Одной связкой, чтобы снимок вида подставлял состояние руками
 * так же, как его отдают модели.
 */
class HistoryParts(
    val day: JournalUiState,
    val dayActions: JournalActions = object : JournalActions {},
    val shifts: ShiftsUiState = ShiftsUiState(),
    val shiftActions: ShiftsActions = object : ShiftsActions {},
    val print: PrintActions = object : PrintActions {}
)

/**
 * Журнал документов.
 *
 * Два взгляда на одно и то же: по дню — «что было вчера», по сменам —
 * «покажи Z-отчёт позавчерашней смены». Взгляд переключается сегментами,
 * а не плашками отбора: это выбор одного из двух, а не фильтр, и Material
 * различает эти два случая разными управляющими элементами.
 */
@Composable
fun HistoryContent(parts: HistoryParts) {
    val texts = LocalStrings.current
    val journal = textsOf(LocalLanguage.current).journal.history
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        WrapRow(spacing = Spacing.cardGap) {
            ScreenTitle(texts.sections.history)
            ChoiceSegments(
                options = HistoryView.entries,
                selected = parts.day.view,
                label = { it.title(journal) },
                onSelect = parts.dayActions::show
            )
        }
        when (parts.day.view) {
            HistoryView.Day -> JournalScreen(parts.day, parts.dayActions, parts.print)
            HistoryView.Shifts -> ShiftsScreen(parts.shifts, parts.shiftActions, parts.print)
        }
        // Доставка открытого чека — окном поверх журнала.
        parts.day.delivery?.let { ReceiptDeliveryDialog(it, parts.dayActions) }
    }
}
