package kz.mybrain.superkassa.presentation.journal.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.HistoryLayout
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.journal.QueueJournalTexts

/**
 * Число ждущих задач.
 *
 * У очереди, о которой касса не ответила, его нет: крупный ноль над пустым
 * экраном владелец читает как порядок, которого никто не подтверждал.
 */
internal fun waitingText(read: Boolean, waiting: Int): String =
    if (read) waiting.toString() else Glyphs.DASH

/**
 * Глубина очереди и повтор.
 *
 * Число ждущих набрано крупно: его читают с метра, не наклоняясь к экрану.
 * Кнопка повтора живёт только тогда, когда есть что повторять — касса
 * повторяет неудачные задачи, а не ждущие своей очереди, — и объяснение
 * стоит рядом с ней, а не отдельной строкой в другом конце экрана.
 */
@Composable
internal fun QueueSummary(state: QueueUiState, actions: QueueActions, journal: QueueJournalTexts) {
    val texts = LocalStrings.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        // Кнопки переносятся под объяснение целиком, а не сжимают его
        // в столбик по два слова: карточка стоит в ширину читаемого текста.
        WrapRow(modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding), spacing = Spacing.sectionGap) {
            WaitingCount(state, texts.queue.waiting)
            Text(
                text = summaryNote(state, journal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).widthIn(min = HistoryLayout.summaryNote)
            )
            SummaryButtons(state, actions)
        }
    }
}

/** Повтор неудачных и перечитывание: кнопки переносятся под объяснение целиком. */
@Composable
private fun SummaryButtons(state: QueueUiState, actions: QueueActions) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sectionGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(enabled = state.canRetry, onClick = actions::retryFailed) { Text(texts.queue.retryFailed) }
        IconButton(onClick = actions::refresh) {
            Icon(AppIcons.refresh, contentDescription = texts.common.refresh)
        }
    }
}

/** Число ждущих — крупно, подпись под ним — мелко. */
@Composable
private fun WaitingCount(state: QueueUiState, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(waitingText(state.read, state.waiting.size), style = MaterialTheme.typography.displaySmall)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Объяснение рядом с кнопкой повтора.
 *
 * «Неудачных задач нет» рядом с красной строкой «не будет отправлен»
 * противоречит самой себе: отвергнутое повтор не берёт, но оно на экране
 * есть, и строка это признаёт.
 */
internal fun summaryNote(state: QueueUiState, journal: QueueJournalTexts): String = when {
    // Касса об очереди не ответила: ни числа ждущих, ни «неудачных задач
    // нет» за неё сказать нельзя — о непрочитанном говорит строка на месте списка.
    !state.read -> ""
    // Повтор касса принимает только в режиме программирования. Прежде кнопка
    // нажималась всегда и отвечала протокольным «ККМ должна быть в режиме PROGRAMMING».
    // Смена закрывается раньше входа в режим: в режиме программирования
    // касса её не закроет, а при открытой смене в повторе откажет.
    state.failed.isNotEmpty() && state.kkm?.isShiftOpen == true -> journal.retryNeedsClosedShift
    state.failed.isNotEmpty() && !state.programming -> journal.retryNeedsProgramming
    state.failed.isNotEmpty() -> journal.retryHint
    state.rejected.isNotEmpty() -> journal.nothingToRetryButRejected
    else -> journal.nothingFailed
}
