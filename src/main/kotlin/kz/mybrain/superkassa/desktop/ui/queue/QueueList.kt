package kz.mybrain.superkassa.desktop.ui.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.history.momentText
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.QueueJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/** Ждущие сверху, отправленные под разделителем: это история, а не работа. */
@Composable
internal fun QueueList(
    waiting: List<QueueTask>,
    done: List<QueueTask>,
    journal: QueueJournalTexts,
    language: String,
    modifier: Modifier
) {
    val texts = LocalStrings.current
    ScrollableList(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(waiting) { at, task -> QueueRow(task, texts, journal, stripedAt(at), language) }
        if (done.isNotEmpty()) {
            item {
                Text(
                    text = "${journal.sentSection}: ${done.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.normal, vertical = Spacing.snug)
                )
            }
        }
        itemsIndexed(done) { at, task -> QueueRow(task, texts, journal, stripedAt(at), language) }
    }
}

/**
 * Строка очереди.
 *
 * Задача — заголовок, попытки, время следующей и причина неудачи — подпись
 * под ним, состояние — плашка справа. Причина показана словами узла: по ней
 * обслуживание отличает «ОФД не отвечает» от «чек отвергнут», не читая
 * журналы.
 */
@Composable
private fun QueueRow(
    task: QueueTask,
    texts: AppStrings,
    journal: QueueJournalTexts,
    striped: Boolean,
    language: String
) {
    val state = queueStateOf(task.status)
    RecordRow(
        title = "${journal.task}: ${task.type ?: Glyphs.DASH}",
        support = { QueueSupport(task, state, texts, journal, language) },
        striped = striped,
        trailing = { Chip(stateTitle(state, task.status, texts, journal), stateColor(state)) }
    )
}

/** Попытки и время следующей одной строкой, причина неудачи — под ней. */
@Composable
private fun QueueSupport(
    task: QueueTask,
    state: QueueState,
    texts: AppStrings,
    journal: QueueJournalTexts,
    language: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        Text(attemptsText(task, state, texts, journal), style = MaterialTheme.typography.bodySmall)
        // Причина — на языке кассира и в три строки, а не сплошной стеной:
        // узел присылает её слепленной из трёх языков и с каждой неудачей
        // заворачивает прежний текст в новый, поэтому она бывает длинной.
        task.reason(language)?.let { reason ->
            Text(
                text = "${journal.lastFailure}: $reason",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = REASON_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Сколько строк причины показывать: длиннее кассир всё равно не читает. */
private const val REASON_LINES = 3

private fun attemptsText(
    task: QueueTask,
    state: QueueState,
    texts: AppStrings,
    journal: QueueJournalTexts
): String {
    val attempts = "${texts.queue.attempts}: ${task.attempt ?: 0}"
    val next = task.nextAttemptAt?.takeIf { state.isWaiting } ?: return attempts
    return "$attempts${Glyphs.SEPARATOR}${journal.nextAttempt}: ${momentText(next)}"
}

/**
 * Состояние задачи словами кассира.
 *
 * Названия те же, что у документов: «в очереди» на экране очереди и на
 * экране смены обязаны означать одно и то же.
 */
private fun stateTitle(
    state: QueueState,
    status: String?,
    texts: AppStrings,
    journal: QueueJournalTexts
): String = when (state) {
    QueueState.Queued -> texts.status.queued
    QueueState.Sending -> journal.sending
    QueueState.Failed -> journal.retrying
    QueueState.Sent -> texts.status.delivered
    QueueState.Rejected -> journal.rejectedForGood
    QueueState.Unknown -> status ?: Glyphs.DASH
}

@Composable
private fun stateColor(state: QueueState): Color = when (state) {
    // Красное только там, где отправки не будет. Неудавшаяся попытка
    // с назначенным повтором — обычная жизнь кассы в разрыве связи,
    // и «Отклонён» красным над таким чеком означал бы потерю документа.
    QueueState.Rejected -> StatusColors.refused
    QueueState.Sent -> StatusColors.delivered
    else -> StatusColors.pending
}
