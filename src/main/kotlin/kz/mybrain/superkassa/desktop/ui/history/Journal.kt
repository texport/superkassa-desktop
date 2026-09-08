package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Общее для журнала, возврата и очереди.
 *
 * Три экрана — один товароучётный журнал, разрезанный по задачам кассира,
 * и повторять в каждом пустое состояние, тире и разбор времени незачем.
 * Собственных красок здесь нет: всё берётся ролями схемы.
 */

@Composable
fun JournalLoading(modifier: Modifier = Modifier) {
    val texts = LocalStrings.current
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator()
        Text(
            text = texts.common.loading,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Подложка строки журнала: через одну, чтобы глаз не терял строку. */
@Composable
fun rowTint(striped: Boolean) =
    if (striped) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface

/** Значение, которого нет: одно тире на всю область. */
const val DASH = "—"

/** Время документа по часам этой машины. */
fun momentText(millis: Long?): String =
    millis?.let { MOMENT.format(Instant.ofEpochMilli(it)) } ?: DASH

private val MOMENT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM HH:mm:ss").withZone(ZoneId.systemDefault())
