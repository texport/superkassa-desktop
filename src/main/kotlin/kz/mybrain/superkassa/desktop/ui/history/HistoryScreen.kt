package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Журнал документов.
 *
 * Два взгляда на одно и то же: по дню — «что было вчера», по сменам —
 * «покажи Z-отчёт позавчерашней смены». Взгляд переключается сегментами,
 * а не плашками отбора: это выбор одного из двух, а не фильтр, и Material
 * различает эти два случая разными управляющими элементами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(session: Session) {
    val texts = LocalStrings.current
    val journal = journalTexts(session.language).history
    var view by remember { mutableStateOf(HistoryView.Day) }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(texts.sections.history, style = MaterialTheme.typography.headlineSmall)
            ChoiceSegments(
                options = HistoryView.entries,
                selected = view,
                label = { it.title(journal) }
            ) { view = it }
        }
        when (view) {
            HistoryView.Day -> DayJournal(session)
            HistoryView.Shifts -> PastShiftsView(session)
        }
    }
}

/** Взгляд на журнал: по дню или по сменам. */
enum class HistoryView(val title: (HistoryJournalTexts) -> String) {
    Day({ it.byDay }),
    Shifts({ it.byShift })
}
