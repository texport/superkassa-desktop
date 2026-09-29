package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Перелистывание дня. Вперёд дальше сегодняшнего идти некуда.
 *
 * «Сегодня» — значком в той же строке: в панели списка шириной 360 точек
 * кнопка с надписью вставала столбиком по букве или уходила отдельной
 * строкой, отнимая высоту у списка чеков.
 */
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
        DayButton(AppIcons.earlierDay, journal.earlierDay, !loading) { onDay(day.minus(ONE_DAY)) }
        Text(Dates.day(day), style = MaterialTheme.typography.titleMedium)
        DayButton(AppIcons.laterDay, journal.laterDay, !loading && day < today) { onDay(day.plus(ONE_DAY)) }
        DayButton(AppIcons.today, journal.today, !loading && day != today) { onDay(today) }
    }
}

/** Кнопка-значок перелистывания; её название — для чтения с экрана. */
@Composable
private fun DayButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(enabled = enabled, onClick = onClick) { Icon(icon, contentDescription = label) }
}

private val ONE_DAY = DatePeriod(days = 1)
