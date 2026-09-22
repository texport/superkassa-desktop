package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.issueToken
import kz.mybrain.superkassa.desktop.server.enterProgramming
import kz.mybrain.superkassa.desktop.server.exitProgramming
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.SubsectionTitle
import kz.mybrain.superkassa.desktop.ui.settings.updateOfdToken
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Технический токен кассы.
 *
 * Выдаётся по требованию владельца и показывается один раз: это ключ,
 * которым касса подписывает запросы, и место ему в настройках кассы,
 * а не в журнале кабинета.
 *
 * Кнопка гаснет там, где кабинет токен не выдаст: по черновику и по кассе
 * с поданным заявлением. Прежде она нажималась всегда, и владелец получал
 * отказ сервера — по-английски и кодом.
 *
 * Выданное значение обёрнуто в область выделения: его переносят в другое
 * приложение, а переписывать десять цифр с экрана руками — верный способ
 * ошибиться в одной.
 *
 * Кассе, которая работает на этой же машине, новый токен вписывается сразу.
 * Прежде владелец одним нажатием ломал свою кассу: БФД отзывал прежний
 * токен, узел о новом не знал и на первом же чеке получал «неверный токен»
 * и блокировку — а приложение говорило лишь «Токен выдан». Когда касса
 * здесь, но кассир вошёл в другую, вписать за него нельзя — тогда об этом
 * сказано словами.
 */
@Composable
fun RegisterTokenBlock(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister
) {
    val scope = rememberCoroutineScope()
    var issued by remember(register.id) { mutableStateOf<Long?>(null) }

    val allowed = tokenAllowed(register)
    val here = (nodeWork(register, session.kkms) as? NodeWork.Here)?.kkm
    // Вписать токен можно только в ту кассу, в которую кассир вошёл: пин
    // принадлежит кассе, и чужой к этой не подойдёт.
    val mine = here?.takeIf { it.kkmId == session.selected?.kkmId && session.pin.isNotBlank() }
    SubsectionTitle(texts.token, texts.hints.token)
    // Почему кнопка погасла — строкой: это состояние кассы, а не объяснение
    // раздела, и владелец должен видеть его не открывая подсказку.
    if (!allowed) {
        Text(
            text = texts.tokenOnlyRegistered,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (here != null && mine == null) {
        Text(
            text = texts.tokenNeedsNode,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    issued?.let { value ->
        SelectionContainer {
            DetailLine(texts.tokenIssued, value.toString())
        }
    }
    BusyButton(
        text = texts.issueToken,
        busy = cabinet.busy,
        enabled = allowed,
        onClick = {
            scope.launch {
                val token = cabinet.token ?: return@launch
                val value = cabinet.guard { cabinet.client.issueToken(token, register.id) }?.token
                issued = value
                val kkmId = mine?.kkmId
                if (value != null && kkmId != null) {
                    deliver(session, kkmId, value.toString(), texts)
                }
            }
        }
    )
}

/**
 * Вписывает выданный токен в кассу узла.
 *
 * Замена токена — настройка кассы, и узел принимает её только в режиме
 * программирования: в него входят и выходят здесь же, чтобы владелец
 * не оставил кассу в нём после выдачи токена.
 */
private suspend fun deliver(session: Session, kkmId: String, token: String, texts: CabinetTexts) {
    val pin = session.pin
    val written = session.guard(texts.issueToken) {
        session.client.enterProgramming(kkmId, pin)
        session.client.updateOfdToken(kkmId, token, pin)
        session.client.exitProgramming(kkmId, pin)
    }
    if (written != null) {
        session.report(texts.tokenGoesToNode)
        session.refreshKkms()
    }
}
