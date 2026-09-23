package kz.mybrain.superkassa.presentation.settings

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
import kz.mybrain.superkassa.data.node.NodeHealth
import kz.mybrain.superkassa.data.node.NodeInfo
import kz.mybrain.superkassa.data.node.OfdAuthInfo
import kz.mybrain.superkassa.data.node.nodeHealth
import kz.mybrain.superkassa.data.node.nodeInfo
import kz.mybrain.superkassa.data.node.ofdAuthInfo
import kz.mybrain.superkassa.presentation.components.FactLines
import kz.mybrain.superkassa.presentation.components.SectionCard
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.strings.updateTexts
import kz.mybrain.superkassa.presentation.theme.Glyphs
import kz.mybrain.superkassa.presentation.theme.Spacing

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

    SectionCard(title = texts.node, info = texts.nodeHint) {
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
        auth?.let { FactLines(texts.ofdAuth, authLines(texts.ofdNextReqNum, it), texts.nodeUnknown) }
    }
}

/**
 * Что узел рассказал о себе; неизвестное в перечень не попадает.
 *
 * Первой строкой — версия самой кассы: поддержка спрашивает обе,
 * и разбирать отказ по узлу, не зная, какая касса к нему ходит, нельзя.
 */
@Composable
private fun nodeLines(
    session: Session,
    info: NodeInfo?,
    health: NodeHealth?
): List<Pair<String, String>> {
    val texts = LocalStrings.current.settings
    return listOfNotNull(
        updateTexts(session.language).appName to session.updates.version.label,
        info?.version?.let { texts.nodeVersion to "${info.name.orEmpty()} $it".trim() },
        info?.coreVersion?.let { texts.nodeCoreVersion to it },
        info?.mode?.let { texts.nodeMode to it },
        info?.ofdProtocolVersion?.let { texts.nodeProtocol to it },
        info?.storage?.engine?.let { texts.nodeStorage to it },
        info?.let { texts.nodeRenderer to (it.documentRenderer ?: texts.nodeRendererMissing) },
        health?.status?.let { texts.nodeHealth to listOfNotNull(it, health.storage).joinToString(Glyphs.SEPARATOR) },
        (texts.nodeHealth to LocalStrings.current.common.nodeOffline).takeIf { !session.nodeAvailable }
    )
}

/**
 * Номер следующего запроса — и только он.
 *
 * Рядом стоял токен кассы, как его отдал узел. По нему отправляют
 * фискальные команды от её имени, а экран настроек показывают и
 * снимают: строка с токеном уезжала в чужой снимок вместе со всем,
 * что рядом. Разошедшийся номер запроса объясняет отказы ОФД и сам
 * по себе прав ни на что не даёт.
 */
internal fun authLines(reqNum: String, auth: OfdAuthInfo): List<Pair<String, String>> =
    listOfNotNull(auth.nextReqNum?.let { reqNum to it.toString() })
