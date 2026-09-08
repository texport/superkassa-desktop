package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.ApplicationSent
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.SignRequest
import kz.mybrain.superkassa.desktop.server.cabinet.prepareRegistration
import kz.mybrain.superkassa.desktop.server.cabinet.register
import kz.mybrain.superkassa.desktop.server.cabinet.signRegistration
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Шаг 3: постановка кассы на учёт в ИСНА.
 *
 * Заявление готовит кабинет, подписывает владелец ключом ЭЦП, отправляет
 * снова кабинет. Ответ ИСНА приходит не в ту же минуту, поэтому шаг
 * не притворяется завершённым: он показывает состояние кассы в кабинете
 * и даёт перечитать его, когда владелец вернётся.
 */
@Composable
fun ApplicationStepCard(
    session: Session,
    cabinet: CabinetSession,
    setup: SetupTexts,
    draft: KkmSetupDraft,
    onRegistered: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var card by remember(draft.cabinetRegisterId) { mutableStateOf<CabinetRegister?>(null) }

    suspend fun reload() {
        val token = cabinet.token ?: return
        val id = draft.cabinetRegisterId ?: return
        card = cabinet.guard { cabinet.client.register(token, id) }
    }

    LaunchedEffect(draft.cabinetRegisterId, cabinet.token) { reload() }

    val onRecord = card?.registrationNumber?.isNotBlank() == true
    // Следующий шаг узнаёт о постановке на учёт отсюда, а не перечитыванием
    // по таймеру: ответ ИСНА приходит когда придёт.
    LaunchedEffect(onRecord) { if (onRecord) onRegistered() }
    SetupStepCard(
        title = setup.stepApplication,
        hint = setup.stepApplicationHint,
        texts = setup,
        done = onRecord,
        ready = draft.cabinetRegisterId != null && cabinet.open,
        summary = listOfNotNull(setup.registered, card?.registrationNumber).joinToString(" · ")
    ) {
        if (onRecord) return@SetupStepCard
        Text(
            // Состояние кассы — это ещё не состояние заявления: пока
            // заявления нет, «ждём ответа ИСНА» над «DRAFT» просто врёт.
            text = card?.status?.let { "${setup.status} $it" } ?: setup.stepApplicationHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusyButton(
                text = setup.submit,
                busy = cabinet.busy,
                onClick = {
                    scope.launch {
                        // Перечитывание только по удаче: guard снимает
                        // сообщение в начале обращения, и отказ подачи
                        // стирался прежде, чем владелец успевал прочитать.
                        if (submit(cabinet, draft) != null) {
                            reload()
                        }
                    }
                }
            )
            TextButton(onClick = { scope.launch { reload() } }) {
                Text(cabinetTexts(session.language).refresh)
            }
        }
    }
}

/**
 * Готовит заявление, отдаёт его на подпись и отправляет подписанное.
 *
 * Если владелец закроет окно NCALayer, заявление останется черновиком
 * в кабинете: фискального следа это не оставляет, и подать его можно
 * заново.
 */
private suspend fun submit(cabinet: CabinetSession, draft: KkmSetupDraft): ApplicationSent? {
    val token = cabinet.token ?: return null
    val id = draft.cabinetRegisterId ?: return null
    return cabinet.guard {
        val prepared = cabinet.client.prepareRegistration(token, id)
        val signature = cabinet.sign(prepared.payloadToSign)
        cabinet.client.signRegistration(token, id, SignRequest(prepared.actionId, signature))
    }
}
