package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.pingOfd
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.FactLines
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Диагностика кассы: связь с ОФД, сведения о нём и об узле.
 *
 * Режим программирования отсюда убран: выход из него стоял здесь, а вход —
 * двумя разделами выше, и кассир, вошедший в режим, искал выход по всему
 * экрану. И то и другое живёт теперь под самой кассой, в [ProgrammingCard].
 *
 * Проверки гаснут, пока узел молчит: спрашивать о связи с БФД ту самую
 * службу, которая не отвечает, незачем — кассир получал отказ после
 * нажатия и узнавал из него ровно то, что и так видно по состоянию узла.
 */
@Composable
fun DiagnosticsCard(session: Session) {
    val texts = LocalStrings.current
    val money = moneyTexts(session.language).kkm
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<OfdSummary?>(null) }
    var node by remember { mutableStateOf<NodeFacts?>(null) }
    var linkAlive by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val body = session.guard(money.nodeTitle) { session.client.nodeSettingsBody() }
        node = body?.let(::parseNodeSettings)
    }

    val ready = session.selected != null && !busy && session.nodeAvailable

    SectionCard(
        title = texts.settings.diagnostics,
        info = money.diagnosticsHint,
        // Ответ ОФД — не состояние кассы, а итог только что нажатой
        // проверки: он и остаётся здесь, в строке заголовка.
        trailing = {
            linkAlive?.let { alive ->
                if (alive) {
                    Chip(texts.settings.ofdAnswers, StatusColors.delivered)
                } else {
                    Chip(texts.settings.ofdSilent, StatusColors.refused)
                }
            }
        }
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                enabled = ready,
                onClick = {
                    scope.launch {
                        busy = true
                        linkAlive = pingLink(session, texts.settings.ofdLink)
                        busy = false
                    }
                }
            ) { Text(texts.settings.checkOfdLink) }
            OutlinedButton(
                enabled = ready,
                onClick = {
                    scope.launch {
                        busy = true
                        summary = askOfd(session, texts.settings.ofdInfo)
                        busy = false
                    }
                }
            ) { Text(texts.settings.ofdInfo) }
        }

        if (summary == null && node == null) {
            Text(
                text = money.diagnosticsEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        summary?.let { FactLines(texts.settings.ofdInfo, it.rows(money), money.ofdEmpty) }
        node?.let { FactLines(money.nodeTitle, it.rows(money), money.ofdEmpty) }
    }
}

private suspend fun pingLink(session: Session, what: String): Boolean? {
    val kkm = session.selected ?: return null
    return session.guard(what) { session.client.pingOfd(kkm.kkmId, session.pin) }
}

private suspend fun askOfd(session: Session, what: String): OfdSummary? {
    val kkm = session.selected ?: return null
    val body = session.guard(what) { session.client.ofdInfoBody(kkm.kkmId, session.pin) }
    return body?.let { parseOfdInfo(it, session.language.code) }
}
