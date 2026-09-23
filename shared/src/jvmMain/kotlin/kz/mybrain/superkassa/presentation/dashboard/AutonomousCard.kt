package kz.mybrain.superkassa.presentation.dashboard

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
import kz.mybrain.superkassa.domain.kkm.isAutonomous
import kz.mybrain.superkassa.presentation.components.Chip
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.Spacing
import kz.mybrain.superkassa.presentation.theme.StatusColors

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
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            AutonomousHead(kkm.offlineQueueCount)
            Text(texts.autonomous.explain, style = MaterialTheme.typography.bodySmall)
            // Отрицательный ответ проверки объявляется так же, как
            // положительный: кассир нажал и обязан узнать итог.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug)) {
                TextButton(onClick = actions::checkLink) { Text(texts.autonomous.checkLink) }
                TextButton(onClick = actions::sendQueued) { Text(texts.autonomous.sendQueued) }
            }
            // Условия досылки названы заранее: без них кассир жмёт кнопку,
            // которая при открытой смене не может сработать никогда.
            Text(
                texts.autonomous.sendQueuedRules,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Автономный режим и сколько документов ждут отправки. */
@Composable
private fun AutonomousHead(waiting: Int) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip(texts.autonomous.title, StatusColors.pending)
        Text("${texts.autonomous.waiting}: $waiting", style = MaterialTheme.typography.bodyMedium)
    }
}
