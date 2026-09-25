package kz.mybrain.superkassa.presentation.kassa.refund.component

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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.period.text
import kz.mybrain.superkassa.presentation.common.period.workplaceToday
import kz.mybrain.superkassa.strings.api.journal.HistoryJournalTexts

/** Перелистывание дня. Вперёд дальше сегодняшнего идти некуда. */
@Composable
internal fun DayBar(
    journal: HistoryJournalTexts,
    day: LocalDate,
    loading: Boolean,
    onDay: (LocalDate) -> Unit
) {
    val today = workplaceToday()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = journal.day,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(enabled = !loading, onClick = { onDay(day.minus(ONE_DAY)) }) {
            Icon(AppIcons.earlierDay, contentDescription = journal.earlierDay)
        }
        Text(Dates.day(day), style = MaterialTheme.typography.titleMedium)
        IconButton(enabled = !loading && day < today, onClick = { onDay(day.plus(ONE_DAY)) }) {
            Icon(AppIcons.laterDay, contentDescription = journal.laterDay)
        }
        TextButton(enabled = !loading && day != today, onClick = { onDay(today) }) {
            Icon(AppIcons.today, contentDescription = null)
            Text(journal.today, modifier = Modifier.padding(start = Spacing.itemGap))
        }
    }
}

private val ONE_DAY = DatePeriod(days = 1)
