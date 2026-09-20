package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.FactLines
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Чем отвечает узел на этой машине.
 *
 * Версия, режим, версия протокола и хранилище — первое, что спрашивает
 * поддержка при разборе. Раньше их негде было увидеть: строка «узел
 * недоступен» не называет ни версии, ни того, какая часть узла легла.
 *
 * Сведения читаются сами при открытии настроек: они не меняются под
 * руками и нажимать ради них нечего. Данные авторизации ОФД — отдельным
 * действием: они требуют пина и нужны редко.
 */
@Composable
fun NodeFactsCard(session: Session) {
    val texts = LocalStrings.current.settings
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<NodeInfo?>(null) }
    var health by remember { mutableStateOf<NodeHealth?>(null) }
    var auth by remember { mutableStateOf<OfdAuthInfo?>(null) }

    LaunchedEffect(session.nodeAvailable) {
        info = session.guard(texts.node) { session.client.nodeInfo() }
        health = session.guard(texts.node) { session.client.nodeHealth() }
    }

    SectionCard(title = texts.node) {
        FactLines(texts.diagnostics, nodeLines(session, info, health), texts.nodeUnknown)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
            OutlinedButton(
                enabled = !session.busy && session.selected != null,
                onClick = {
                    scope.launch {
                        val kkm = session.selected ?: return@launch
                        auth = session.guard(texts.ofdAuth) {
                            session.client.ofdAuthInfo(kkm.kkmId, session.pin)
                        }
                    }
                }
            ) { Text(texts.ofdAuth) }
        }
        auth?.let { FactLines(texts.ofdAuth, authLines(texts.ofdNextReqNum, texts.ofdToken, it), texts.nodeUnknown) }
    }
}

/** Что узел рассказал о себе; неизвестное в перечень не попадает. */
@Composable
private fun nodeLines(
    session: Session,
    info: NodeInfo?,
    health: NodeHealth?
): List<Pair<String, String>> {
    val texts = LocalStrings.current.settings
    return listOfNotNull(
        info?.version?.let { texts.nodeVersion to "${info.name.orEmpty()} $it".trim() },
        info?.coreVersion?.let { texts.nodeCoreVersion to it },
        info?.mode?.let { texts.nodeMode to it },
        info?.ofdProtocolVersion?.let { texts.nodeProtocol to it },
        info?.storage?.engine?.let { texts.nodeStorage to it },
        health?.status?.let { texts.nodeHealth to listOfNotNull(it, health.storage).joinToString(Glyphs.SEPARATOR) },
        (texts.nodeHealth to LocalStrings.current.common.nodeOffline).takeIf { !session.nodeAvailable }
    )
}

/** Номер следующего запроса и токен, как их отдал узел. */
private fun authLines(reqNum: String, token: String, auth: OfdAuthInfo): List<Pair<String, String>> =
    listOfNotNull(
        auth.nextReqNum?.let { reqNum to it.toString() },
        auth.token?.takeIf { it.isNotBlank() }?.let { token to it }
    )
