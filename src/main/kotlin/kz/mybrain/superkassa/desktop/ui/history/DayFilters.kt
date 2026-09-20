package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.LocalDate

/** Перелистывание дня. Вперёд дальше сегодняшнего идти некуда. */
@Composable
internal fun DayBar(
    journal: HistoryJournalTexts,
    day: LocalDate,
    loading: Boolean,
    onDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = journal.day,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(enabled = !loading, onClick = { onDay(day.minusDays(1)) }) {
            Icon(AppIcons.earlierDay, contentDescription = journal.earlierDay)
        }
        Text(JOURNAL_DAY.format(day), style = MaterialTheme.typography.titleMedium)
        IconButton(enabled = !loading && day < today, onClick = { onDay(day.plusDays(1)) }) {
            Icon(AppIcons.laterDay, contentDescription = journal.laterDay)
        }
        TextButton(enabled = !loading && day != today, onClick = { onDay(today) }) {
            Icon(AppIcons.today, contentDescription = null)
            Text(journal.today, modifier = Modifier.padding(start = Spacing.tight))
        }
    }
}
