package kz.mybrain.superkassa.desktop.ui.dashboard

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.server.pingOfd
import kz.mybrain.superkassa.desktop.server.retryFailedQueue
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Что происходит, когда связи с ОФД нет.
 *
 * Кассир должен понимать три вещи: работать можно, чеки не потеряны и
 * уйдут сами. Без этого автономный режим выглядит как поломка, и касса
 * простаивает, пока ждут «когда починится».
 */
@Composable
internal fun AutonomousCard(session: Session) {
    val texts = LocalStrings.current
    val kkm = session.selected ?: return
    if (!kkm.isAutonomous) return
    val scope = rememberCoroutineScope()
    val waiting = session.queueTasks.count { it.isWaiting }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(texts.autonomous.title, StatusColors.pending)
                Text(
                    "${texts.autonomous.waiting}: $waiting",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(texts.autonomous.explain, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug)) {
                TextButton(onClick = {
                    scope.launch {
                        val alive = session.guard<Boolean>(texts.autonomous.checkLink) {
                            session.client.pingOfd(kkm.kkmId, session.pin)
                        }
                        // Сначала перечитать, потом объявить: `guard` гасит
                        // сообщение в начале каждого вызова, и объявленный
                        // до обновления итог кассир не успевал увидеть.
                        session.refreshKkms()
                        // Отрицательный ответ объявляется так же, как
                        // положительный: кассир нажал и обязан узнать итог.
                        // Молчание он читает как «кнопка не работает».
                        when (alive) {
                            true -> session.report(texts.autonomous.linkBack)
                            false -> session.report(texts.settings.ofdSilent)
                            null -> Unit
                        }
                    }
                }) { Text(texts.autonomous.checkLink) }
                TextButton(onClick = {
                    scope.launch {
                        val sent = session.guard(texts.queue.retryFailed) {
                            session.client.retryFailedQueue(kkm.kkmId, session.pin)
                        }
                        session.refreshSelected()
                        if (sent != null) session.report(texts.queue.retryDone)
                    }
                }) { Text(texts.autonomous.sendQueued) }
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
