package kz.mybrain.superkassa.desktop.ui.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.QueueJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Глубина очереди и повтор.
 *
 * Число ждущих набрано крупно: его читают с метра, не наклоняясь к экрану.
 * Кнопка повтора живёт только тогда, когда есть что повторять — узел
 * повторяет неудачные задачи, а не ждущие своей очереди, — и объяснение
 * стоит рядом с ней, а не отдельной строкой в другом конце экрана.
 */
@Composable
internal fun QueueSummary(
    session: Session,
    journal: QueueJournalTexts,
    waiting: Int,
    hasFailed: Boolean,
    hasRejected: Boolean
) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            horizontalArrangement = Arrangement.spacedBy(Spacing.roomy),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(waiting.toString(), style = MaterialTheme.typography.displaySmall)
                Text(
                    text = texts.queue.waiting,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                // «Неудачных задач нет» рядом с красной строкой «не будет
                // отправлен» противоречит самой себе: отвергнутое повтор
                // не берёт, но оно на экране есть, и строка это признаёт.
                text = when {
                    hasFailed -> journal.retryHint
                    hasRejected -> journal.nothingToRetryButRejected
                    else -> journal.nothingFailed
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(
                enabled = session.selected != null && hasFailed,
                onClick = { scope.launch { retryQueue(session, texts) } }
            ) { Text(texts.queue.retryFailed) }
            IconButton(onClick = { scope.launch { session.refreshSelected() } }) {
                Icon(AppIcons.refresh, contentDescription = texts.common.refresh)
            }
        }
    }
}
