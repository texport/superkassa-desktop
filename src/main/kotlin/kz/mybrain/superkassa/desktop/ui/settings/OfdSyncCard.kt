package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.KkmSetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Сверка кассы с БФД.
 *
 * Всё, что касса знает о себе, приходит от БФД: организация, адрес,
 * регистрационные номера, счётчики и номер смены. Разойтись они могут
 * после автономной работы или замены сведений в кабинете БФД — тогда
 * кассир сверяет их отсюда, а не переустанавливает кассу.
 *
 * Аббревиатура расшифрована в подсказке у заголовка — один раз
 * на приложение, а не в каждой надписи, где БФД упомянута.
 *
 * Условие стоит под своей кнопкой, а не общим списком внизу: у сверки
 * сведений и сверки счётчиков требования разные, и общий список заставлял
 * бы вспоминать, какое из них к чему.
 *
 * Кнопка гаснет там, где узел заведомо откажет: пока он молчит, пока
 * в очереди лежат неотправленные документы и — у сверки сведений —
 * пока смена открыта. Условие написано под значком у той же кнопки,
 * и отказ после нажатия не сообщал кассиру ничего нового.
 */
@Composable
fun OfdSyncCard(session: Session) {
    val money = moneyTexts(session.language).kkm
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val ready = session.selected != null && !busy && session.nodeAvailable && session.queueTasks.isEmpty()

    SectionCard(title = money.syncTitle, info = money.bfdMeaning) {
        SyncAction(
            title = money.syncService,
            hint = money.syncServiceHint,
            enabled = ready && !session.shiftOpen,
            onClick = {
                scope.launch {
                    busy = true
                    sync(session, money, service = true)
                    busy = false
                }
            }
        )
        SyncAction(
            title = money.syncCounters,
            hint = money.syncCountersHint,
            enabled = ready,
            onClick = {
                scope.launch {
                    busy = true
                    sync(session, money, service = false)
                    busy = false
                }
            }
        )
    }
}

/**
 * Кнопка сверки и условие, при котором узел её выполнит.
 *
 * Условие — под значком: строкой во всю ширину оно занимало у каждой
 * кнопки по две строки экрана, а читают его один раз.
 */
@Composable
private fun SyncAction(title: String, hint: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onClick, enabled = enabled) { Text(title) }
        InfoTip(hint)
    }
}

private suspend fun sync(session: Session, money: KkmSetupTexts, service: Boolean) {
    val kkm = session.selected ?: return
    val what = if (service) money.syncService else money.syncCounters
    session.guard(what) {
        if (service) {
            session.client.syncOfdServiceInfo(kkm.kkmId, session.pin)
        } else {
            session.client.syncOfdCounters(kkm.kkmId, session.pin)
        }
    } ?: return
    session.refreshKkms()
    session.refreshSelected()
    session.report(if (service) money.syncServiceDone else money.syncCountersDone)
}
