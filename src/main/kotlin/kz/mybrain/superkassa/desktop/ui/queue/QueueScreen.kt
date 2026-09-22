package kz.mybrain.superkassa.desktop.ui.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.retryFailedQueue
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScreenTitle
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
    val journal = journalTexts(session.language).queue
    val waiting = waitingTasks(session.queueTasks)
    val failed = failedTasks(session.queueTasks)
    val rejected = rejectedTasks(session.queueTasks)
    val sent = sentTasks(session.queueTasks)

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        ScreenTitle(texts.queue.title)
        QueueSummary(session, journal, waiting.size, failed.isNotEmpty(), rejected.isNotEmpty())
        val state = when {
            session.queueTasks.isNotEmpty() -> ScreenState.Ready
            session.busy -> ScreenState.Working
            else -> ScreenState.Empty(AppIcons.queueClear, texts.queue.empty, journal.emptyHint)
        }
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

internal suspend fun retryQueue(session: Session, texts: AppStrings) {
    val kkm = session.selected ?: return
    session.guard(texts.queue.retryFailed) {
        session.client.retryFailedQueue(kkm.kkmId, session.pin)
    } ?: return
    session.report(texts.queue.retryDone)
    session.refreshSelected()
}
