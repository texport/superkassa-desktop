package kz.mybrain.superkassa.presentation.shift.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kkm.model.isAutonomous
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardActions
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardUiState

/**
 * Что происходит, когда связи с ОФД нет.
 *
 * Кассир должен понимать три вещи: работать можно, чеки не потеряны и
 * уйдут сами. Без этого автономный режим выглядит как поломка, и касса
 * простаивает, пока ждут «когда починится».
 */
@Composable
internal fun AutonomousCard(state: DashboardUiState, actions: DashboardActions) {
    val texts = LocalStrings.current
    val kkm = state.kkm ?: return
    if (!kkm.isAutonomous) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            AutonomousHead(kkm.offlineQueueCount)
            Text(texts.autonomous.explain, style = MaterialTheme.typography.bodySmall)
            QueueActions(state, actions)
        }
    }
}

/**
 * Проверка связи — всегда; досылка — только когда касса её примет,
 * а иначе вместо кнопки названы её условия: накопленное уходит само.
 * Отрицательный ответ проверки объявляется так же, как положительный:
 * кассир нажал и обязан узнать итог.
 */
@Composable
private fun QueueActions(state: DashboardUiState, actions: DashboardActions) {
    val texts = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        TextButton(onClick = actions::checkLink) { Text(texts.autonomous.checkLink) }
        if (state.canSendQueued) {
            TextButton(onClick = actions::sendQueued) { Text(texts.autonomous.sendQueued) }
        }
    }
    if (!state.canSendQueued) {
        Text(
            texts.autonomous.sendQueuedRules,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Автономный режим и сколько документов ждут отправки. */
@Composable
private fun AutonomousHead(waiting: Int) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip(texts.autonomous.title, StatusColors.pending)
        Text("${texts.autonomous.waiting}: $waiting", style = MaterialTheme.typography.bodyMedium)
    }
}
