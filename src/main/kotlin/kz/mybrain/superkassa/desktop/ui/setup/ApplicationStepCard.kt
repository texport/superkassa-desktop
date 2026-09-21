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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import kz.mybrain.superkassa.desktop.ui.cabinet.ApplicationSignWait
import kz.mybrain.superkassa.desktop.ui.cabinet.ApplicationStage
import kz.mybrain.superkassa.desktop.ui.cabinet.statusTitle
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Шаг 3: постановка кассы на учёт в ИСНА.
 *
 * Заявление готовит кабинет, подписывает владелец ключом ЭЦП, отправляет
 * снова кабинет. Ответ ИСНА приходит не в ту же минуту, поэтому шаг
 * не притворяется завершённым: он показывает состояние кассы в кабинете
 * и перечитывает его сам, пока номера ещё нет. «Обновить» остаётся — им
 * спрашивают, не дожидаясь очередного круга.
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
    var stage by remember(draft.cabinetRegisterId) { mutableStateOf<ApplicationStage?>(null) }
    // Начатая подача: ею же владелец прерывает ожидание подписи.
    var running by remember(draft.cabinetRegisterId) { mutableStateOf<Job?>(null) }

    suspend fun reload() {
        val token = cabinet.token ?: return
        val id = draft.cabinetRegisterId ?: return
        card = cabinet.guard { cabinet.client.register(token, id) }
    }

    LaunchedEffect(draft.cabinetRegisterId, cabinet.token) { reload() }

    val onRecord = card?.registrationNumber?.isNotBlank() == true
    LaunchedEffect(onRecord) { if (onRecord) onRegistered() }
    // Пока номера нет, состояние перечитывается само: ответ КГД приходит
    // через десятки секунд, и владелец сидел над шагом, нажимая «Обновить»,
    // чтобы узнать, рассмотрено ли заявление. Отсчёт живёт вместе с шагом:
    // закрытый мастер опроса не продолжает, а полученный номер его кончает.
    LaunchedEffect(onRecord, draft.cabinetRegisterId, cabinet.token) {
        if (onRecord || draft.cabinetRegisterId == null) return@LaunchedEffect
        while (true) {
            delay(Durations.whileWatching)
            reload()
        }
    }
    SetupStepCard(
        title = setup.stepApplication,
        hint = setup.stepApplicationHint,
        texts = setup,
        done = onRecord,
        ready = draft.cabinetRegisterId != null && cabinet.open,
        summary = listOfNotNull(setup.registered, card?.registrationNumber).joinToString(Glyphs.SEPARATOR)
    ) {
        if (onRecord) return@SetupStepCard
        // Состояние кассы — это ещё не состояние заявления: пока заявления
        // нет, «ждём ответа КГД» над черновиком просто врёт. Называется оно
        // словами: здесь стояло «Состояние кассы: DRAFT». Пока карточка
        // не прочитана, строки нет вовсе — на её месте повторялась
        // подсказка самого шага, уже написанная выше.
        card?.status?.let { code ->
            Text(
                text = "${setup.status} ${statusTitle(code, cabinetTexts(session.language))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Пока NCALayer ждёт подпись, на месте кнопок идёт отсчёт срока
        // с отменой — тот же, что на двери входа и при подаче из кабинета.
        if (stage == ApplicationStage.Signing) {
            ApplicationSignWait(session.language, cabinetTexts(session.language)) { running?.cancel() }
            return@SetupStepCard
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusyButton(
                text = setup.submit,
                busy = cabinet.busy,
                onClick = {
                    running = scope.launch {
                        // Перечитывание только по удаче: guard снимает
                        // сообщение в начале обращения, и отказ подачи
                        // стирался прежде, чем владелец успевал прочитать.
                        val sent = try {
                            submit(cabinet, draft) { stage = it }
                        } finally {
                            // Отсчёт снимается и с отменённой подачи.
                            stage = null
                        }
                        if (sent != null) reload()
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
 * Если владелец закроет окно NCALayer или прервёт ожидание подписи,
 * заявление останется черновиком в кабинете: подпись стоит перед
 * отправкой, и в КГД ничего не уходит. Подать его можно заново.
 *
 * @param onStage чего ждут сейчас: кабинета, владельца с ключом или снова
 *   кабинета. По шагу подписи на экране идёт отсчёт срока.
 */
private suspend fun submit(
    cabinet: CabinetSession,
    draft: KkmSetupDraft,
    onStage: (ApplicationStage) -> Unit
): ApplicationSent? {
    val token = cabinet.token ?: return null
    val id = draft.cabinetRegisterId ?: return null
    return cabinet.guard {
        onStage(ApplicationStage.Preparing)
        val prepared = cabinet.client.prepareRegistration(token, id)
        onStage(ApplicationStage.Signing)
        val signature = cabinet.sign(prepared.payloadToSign)
        onStage(ApplicationStage.Sending)
        cabinet.client.signRegistration(token, id, SignRequest(prepared.actionId, signature))
    }
}
