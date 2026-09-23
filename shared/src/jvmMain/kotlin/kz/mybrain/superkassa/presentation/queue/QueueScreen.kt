package kz.mybrain.superkassa.presentation.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.data.node.Dictionary
import kz.mybrain.superkassa.data.node.retryFailedQueue
import kz.mybrain.superkassa.presentation.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.components.ScreenSlot
import kz.mybrain.superkassa.presentation.components.ScreenState
import kz.mybrain.superkassa.presentation.components.ScreenTitle
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.session.refreshSelected
import kz.mybrain.superkassa.presentation.session.titleOf
import kz.mybrain.superkassa.presentation.strings.AppStrings
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.strings.QueueJournalTexts
import kz.mybrain.superkassa.presentation.strings.journalTexts
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Очередь отложенной отправки.
 *
 * Разделены ждущие, отправленные и отвергнутые: глубина очереди — это
 * только ждущие, и она вынесена наверх крупным числом, потому что это
 * единственное, ради чего кассир сюда заходит. Отправленные остаются
 * перечнем как история, а отвергнутые — своей частью: их не повторяют,
 * и к отправленным они не относятся.
 */
@Composable
fun QueueScreen(session: Session) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    val journal = journalTexts(session.language).queue
    val waiting = waitingTasks(session.queueTasks)
    val failed = failedTasks(session.queueTasks)
    val rejected = rejectedTasks(session.queueTasks)
    val sent = sentTasks(session.queueTasks)

    // Очередь — перечень задач с причинами в три строки: во всю ширину
    // монитора плашка состояния уезжала от названия задачи на полторы
    // тысячи точек, а строку причины глаз терял на переносе.
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen).contentWidth(ContentKind.Reading),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        ScreenTitle(texts.queue.title)
        QueueSummary(
            session = session,
            journal = journal,
            waiting = waiting.size,
            read = session.queueRead,
            hasFailed = failed.isNotEmpty(),
            hasRejected = rejected.isNotEmpty()
        )
        val state = queueState(
            texts = texts,
            journal = journal,
            tasks = session.queueTasks.size,
            busy = session.busy,
            read = session.queueRead,
            blocked = session.selected?.isBlocked == true,
            onRetry = { scope.launch { session.refreshSelected() } }
        )
        ScreenSlot(state, Modifier.weight(1f)) {
            QueueList(
                waiting = waiting,
                sent = sent,
                rejected = rejected,
                journal = journal,
                language = session.language.code,
                taskTitle = { session.titleOf(Dictionary.DocumentTypes, it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Что стоит на месте списка задач.
 *
 * «Ждущих документов нет — всё доставлено» — утверждение о кассе, и делать
 * его можно только вслед за ответом узла. Узел, который отказал или
 * промолчал, об очереди не сказал ничего: владелец читал его молчание
 * как порядок и уходил с экрана уверенный, что касса на связи.
 *
 * Пустая очередь заблокированной кассы — тоже не признак порядка:
 * документы не ждут отправки потому, что их больше не пробить.
 */
internal fun queueState(
    texts: AppStrings,
    journal: QueueJournalTexts,
    tasks: Int,
    busy: Boolean,
    read: Boolean,
    blocked: Boolean,
    onRetry: () -> Unit
): ScreenState = when {
    tasks > 0 -> ScreenState.Ready
    busy -> ScreenState.Working
    !read -> ScreenState.Trouble(journal.unread, journal.unreadHint, onRetry)
    else -> ScreenState.Empty(
        AppIcons.queueClear,
        texts.queue.empty,
        if (blocked) journal.emptyBlockedHint else journal.emptyHint
    )
}

internal suspend fun retryQueue(session: Session, texts: AppStrings) {
    val kkm = session.selected ?: return
    session.guard(texts.queue.retryFailed) {
        session.client.retryFailedQueue(kkm.kkmId, session.pin)
    } ?: return
    session.report(texts.queue.retryDone)
    session.refreshSelected()
}
