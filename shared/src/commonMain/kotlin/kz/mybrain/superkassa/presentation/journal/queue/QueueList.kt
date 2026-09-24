package kz.mybrain.superkassa.presentation.journal.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import kz.mybrain.superkassa.domain.journal.model.QueueState
import kz.mybrain.superkassa.domain.journal.model.reason
import kz.mybrain.superkassa.domain.journal.model.state
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.list.RecordRow
import kz.mybrain.superkassa.presentation.common.list.ScrollableList
import kz.mybrain.superkassa.presentation.common.list.stripedAt
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.journal.QueueJournalTexts

/**
 * Ждущие сверху, под ними — отправленные и отвергнутые: это история,
 * а не работа.
 *
 * Отвергнутые стоят своей частью, а не среди отправленных: под заголовком
 * «Уже отправлено» они несли плашку «Не будет отправлен», и заголовок
 * спорил со строкой под ним, а счёт отправленных включал то, что не ушло.
 */
@Composable
internal fun QueueList(state: QueueUiState, journal: QueueJournalTexts, modifier: Modifier) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    // Вид задачи словами кассира: в строке стоял код кассы — «Задача: TICKET».
    val words = QueueWords(texts, journal, language.code) { code ->
        code?.let { state.documentTypes[it]?.of(language) ?: texts.enums.documentFallback(it) ?: it } ?: Glyphs.DASH
    }
    ScrollableList(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(state.waiting) { at, task -> QueueRow(task, words, stripedAt(at)) }
        section(journal.rejectedSection, state.rejected, words)
        section(journal.sentSection, state.sent, words)
    }
}

/**
 * Слова строки очереди: надписи, язык кассира и название вида задачи.
 *
 * @property taskTitle вид задачи словами кассира, а не кодом кассы.
 */
private class QueueWords(
    val texts: AppStrings,
    val journal: QueueJournalTexts,
    val language: String,
    val taskTitle: (String?) -> String
)

/** Часть списка со своим заголовком; пустая часть на экран не выходит. */
private fun LazyListScope.section(title: String, tasks: List<QueueItemResponse>, words: QueueWords) {
    if (tasks.isEmpty()) return
    item {
        Text(
            text = "$title: ${tasks.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.fieldGap)
        )
    }
    itemsIndexed(tasks) { at, task -> QueueRow(task, words, stripedAt(at)) }
}

/**
 * Строка очереди.
 *
 * Задача — заголовок, попытки, время следующей и причина неудачи — подпись
 * под ним, состояние — плашка справа. Причина показана словами кассы: по ней
 * обслуживание отличает «БФД не отвечает» от «чек отвергнут», не читая
 * журналы.
 */
@Composable
private fun QueueRow(task: QueueItemResponse, words: QueueWords, striped: Boolean) {
    val state = task.state
    RecordRow(
        title = "${words.journal.task}: ${words.taskTitle(task.type)}",
        support = { QueueSupport(task, state, words.texts, words.journal, words.language) },
        striped = striped,
        trailing = { Chip(stateTitle(state, task.status, words.texts, words.journal), stateColor(state)) }
    )
}

/** Попытки и время следующей одной строкой, причина неудачи — под ней. */
@Composable
private fun QueueSupport(
    task: QueueItemResponse,
    state: QueueState,
    texts: AppStrings,
    journal: QueueJournalTexts,
    language: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.inline)) {
        Text(attemptsText(task, state, texts, journal), style = MaterialTheme.typography.bodySmall)
        // Причина — на языке кассира и в три строки, а не сплошной стеной:
        // с каждой неудачей касса заворачивает прежний текст в новый,
        // поэтому она бывает длинной.
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
    task: QueueItemResponse,
    state: QueueState,
    texts: AppStrings,
    journal: QueueJournalTexts
): String {
    val attempts = "${texts.queue.attempts}: ${task.attempt}"
    val next = task.nextAttemptAt?.takeIf { state.isWaiting } ?: return attempts
    return "$attempts${Glyphs.SEPARATOR}${journal.nextAttempt}: ${Dates.stamp(next)}"
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
