package kz.mybrain.superkassa.presentation.journal.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.ScreenTitle
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.strings.common.AppStrings
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.journal.QueueJournalTexts
import kz.mybrain.superkassa.presentation.strings.journal.journalTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/** Очередь отложенной отправки: состояние — из модели, действия — ей же. */
@Composable
fun QueueScreen(model: QueueViewModel) {
    val state by model.state.collectAsScreenState()
    // Очередь читается, как раздел открыт: досылка идёт в фоне,
    // и прочитанное минуту назад могло уже уйти.
    LaunchedEffect(Unit) { model.refresh() }
    QueueContent(state, model)
}

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
fun QueueContent(state: QueueUiState, actions: QueueActions) {
    val texts = LocalStrings.current
    val journal = journalTexts(LocalLanguage.current).queue
    // Очередь — перечень во всю ширину раздела, как любой список Material 3.
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        ScreenTitle(texts.queue.title)
        QueueSummary(state, actions, journal)
        ScreenSlot(queueState(texts, journal, state, actions), Modifier.weight(1f)) {
            QueueList(state, journal, Modifier.weight(1f))
        }
    }
}

/**
 * Что стоит на месте списка задач.
 *
 * «Ждущих документов нет — всё доставлено» — утверждение о кассе, и делать
 * его можно только вслед за ответом кассы. Касса, которая отказала или
 * промолчала, об очереди не сказала ничего: владелец читал её молчание
 * как порядок и уходил с экрана уверенный, что касса на связи.
 *
 * Пустая очередь заблокированной кассы — тоже не признак порядка:
 * документы не ждут отправки потому, что их больше не пробить.
 */
internal fun queueState(
    texts: AppStrings,
    journal: QueueJournalTexts,
    state: QueueUiState,
    actions: QueueActions
): ScreenState = when {
    state.tasks.isNotEmpty() -> ScreenState.Ready
    state.reading && !state.read -> ScreenState.Working
    !state.read -> ScreenState.Trouble(journal.unread, journal.unreadHint, actions::refresh)
    else -> ScreenState.Empty(
        AppIcons.queueClear,
        texts.queue.empty,
        if (state.blocked) journal.emptyBlockedHint else journal.emptyHint
    )
}
