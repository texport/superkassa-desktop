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
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.server.enterProgramming
import kz.mybrain.superkassa.desktop.server.exitProgramming
import kz.mybrain.superkassa.desktop.server.pingOfd
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.FactLines
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Диагностика кассы: связь с ОФД, сведения о нём, узел и режим программирования.
 *
 * Режим программирования выведен сюда намеренно: настройки кассы и снятие
 * её с учёта узел разрешает только в нём, и кассир должен видеть, включён
 * он сейчас или нет, а не выяснять это отказом. Переключатель один и назван
 * по тому, что произойдёт: два соседних «войти» и «выйти» заставляют читать
 * плашку, чтобы понять, какая из кнопок сейчас что-то изменит.
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

    val programming = session.selected?.state == PROGRAMMING
    val ready = session.selected != null && !busy

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
            // Войти в программирование предлагает та карточка, поля
            // которой без него заперты. Здесь остаётся только выход —
            // другого места для него на экране нет.
            if (programming) {
                OutlinedButton(
                    enabled = ready,
                    onClick = { scope.launch { switchProgramming(session, enter = false) } }
                ) { Text(texts.settings.exitProgramming) }
            }
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

private suspend fun switchProgramming(session: Session, enter: Boolean) {
    val texts = stringsOf(session.language)
    val kkm = session.selected ?: return
    session.guard(texts.settings.programmingMode) {
        if (enter) {
            session.client.enterProgramming(kkm.kkmId, session.pin)
        } else {
            session.client.exitProgramming(kkm.kkmId, session.pin)
        }
    } ?: return
    // Сообщение объявляется последним: перечитывание списка касс снимает
    // предыдущее, и объяви мы итог раньше — кассир остался бы без ответа.
    session.refreshKkms()
    session.report(
        if (enter) texts.settings.enteredProgramming else texts.settings.exitedProgramming
    )
}

/** Состояние кассы, в котором узел разрешает менять её настройки. */
internal const val PROGRAMMING = "PROGRAMMING"
