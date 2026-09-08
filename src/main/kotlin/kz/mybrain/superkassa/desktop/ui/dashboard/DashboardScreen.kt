package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.FiscalResult
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.closeShift
import kz.mybrain.superkassa.desktop.server.openShift
import kz.mybrain.superkassa.desktop.server.pingOfd
import kz.mybrain.superkassa.desktop.server.retryFailedQueue
import kz.mybrain.superkassa.desktop.server.xReport
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.CommonStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Главный экран: состояние выбранной кассы и документы текущей смены.
 *
 * Показывается именно смена, а не весь журнал: кассиру в течение дня нужна
 * своя смена, а история — отдельный раздел.
 */
@Composable
fun DashboardScreen(session: Session) {
    val texts = LocalStrings.current
    val kkm = session.selected
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        if (kkm == null) {
            Text(texts.shell.noKkm, style = MaterialTheme.typography.titleMedium)
            Text(
                texts.login.pickHint,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.normal), modifier = Modifier.fillMaxWidth()) {
            // Состояние кассы и смены стоит в шапке и повторено здесь не будет:
            // одно и то же слово в двух местах экрана расходится на первой же
            // правке. Плиткам остаются числа смены.
            StatCard(
                caption = texts.dashboard.shift,
                value = if (session.shiftOpen) {
                    texts.dashboard.shiftOpenNo.format(kkm.lastShiftNo ?: "—")
                } else {
                    texts.dashboard.shiftClosed
                },
                modifier = Modifier.weight(1f)
            )
            StatCard(texts.dashboard.cashInDrawer, Money.formatTiyn(session.cashInDrawer), Modifier.weight(1f))
            StatCard(texts.dashboard.documentsInShift, session.documents.size.toString(), Modifier.weight(1f))
        }

        AutonomousCard(session)

        ShiftActions(session)

        RefusedDocuments(session)

        ShiftDocuments(session)
    }
}

/**
 * Управление сменой.
 *
 * Закрытие смены — это Z-отчёт, и назван он так, как называет его кассир,
 * а не протокол.
 */
@Composable
private fun ShiftActions(session: Session) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val programming = session.selected?.isProgramming == true
    // В режиме программирования узел фискальных команд не принимает, и смену
    // он в нём не показывает. Предлагать «Открыть смену» поверх открытой
    // смены — толкать кассира на отказ.
    val enabled = !busy && session.selected != null && session.pin.isNotEmpty() && !programming

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Главное действие экрана одно и зависит от состояния смены:
            // закрытую открывают, открытую закрывают. Остальное — тональное.
            if (session.shiftOpen) {
                Button(enabled = enabled, onClick = {
                    busy = true
                    scope.launch {
                        run(session, texts.dashboard.shiftClosedDone, texts.common) {
                            session.client.closeShift(it.kkmId, session.pin)
                        }
                        busy = false
                    }
                }) { Text(texts.dashboard.closeShift) }

                FilledTonalButton(enabled = enabled, onClick = {
                    busy = true
                    scope.launch {
                        run(session, texts.dashboard.xReportDone, texts.common) {
                            session.client.xReport(it.kkmId, session.pin)
                        }
                        busy = false
                    }
                }) { Text(texts.dashboard.xReport) }
            } else if (session.isAdmin) {
                Button(enabled = enabled, onClick = {
                    busy = true
                    scope.launch {
                        run(session, texts.dashboard.shiftOpened, texts.common) {
                            session.client.openShift(it.kkmId, session.pin)
                        }
                        busy = false
                    }
                }) { Text(texts.dashboard.openShift) }
            }
        }
        if (programming) {
            Text(
                texts.settings.enteredProgramming,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (!session.shiftOpen && !session.isAdmin) {
            // Смену открывает администратор: кассиру вместо кнопки,
            // на которую узел ответит отказом, сказано, кого позвать.
            // Администратору строка не нужна — у него есть сама кнопка,
            // а «откройте смену» уже написано в пустом списке документов.
            Text(
                texts.dashboard.openShiftAdmin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Выполняет действие над сменой и объявляет кассиру, чем оно кончилось.
 *
 * Итог собирается из надписей словаря: то же сообщение по-казахски или
 * по-английски нельзя собрать, дописав русский хвост к переведённому
 * началу.
 */
private suspend fun run(
    session: Session,
    done: String,
    texts: CommonStrings,
    action: suspend (Kkm) -> FiscalResult
) {
    val kkm = session.selected ?: return
    val result = session.guard(done) { action(kkm) } ?: return
    session.report(
        when {
            result.isDelivered -> "$done: ${texts.deliveredToOfd}"
            result.isQueued -> "$done: ${texts.queuedNoLink}"
            result.deliveryStatus == null -> done
            else -> "$done. ${texts.deliveryState}: ${result.deliveryStatus}"
        }
    )
    session.refreshKkms()
    session.refreshSelected()
}

@Composable
private fun StatCard(caption: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.normal), verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * Что происходит, когда связи с ОФД нет.
 *
 * Кассир должен понимать три вещи: работать можно, чеки не потеряны и
 * уйдут сами. Без этого автономный режим выглядит как поломка, и касса
 * простаивает, пока ждут «когда починится».
 */
@Composable
private fun AutonomousCard(session: Session) {
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
