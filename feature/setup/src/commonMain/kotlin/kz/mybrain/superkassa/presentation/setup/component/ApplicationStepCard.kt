package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSession
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationActions
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationUiState
import kz.mybrain.superkassa.presentation.words.cabinet.statusTitle
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.setup.SetupTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаг 3: постановка кассы на учёт в ИСНА.
 *
 * Заявление готовит кабинет, подписывает владелец ключом ЭЦП, отправляет
 * снова кабинет. Ответ ИСНА приходит не в ту же минуту, поэтому шаг
 * не притворяется завершённым: он показывает состояние кассы в кабинете,
 * а мастер перечитывает его сам, пока номера ещё нет. «Обновить» остаётся —
 * им спрашивают, не дожидаясь очередного круга.
 *
 * @param registerId касса в кабинете, которую ставят на учёт; `null` — её ещё нет.
 * @param cabinet шаги кабинета окна: ожидание подписи — то же, что на двери входа.
 * @param session кабинет сейчас: без входа подавать некому, а занятый
 *   обращением кабинет держит кнопку в ожидании.
 */
@Composable
internal fun ApplicationStepCard(
    registerId: String?,
    state: RegistrationUiState,
    actions: RegistrationActions,
    setup: SetupTexts,
    cabinet: CabinetSteps,
    session: CabinetSession
) {
    val record = state.recordOf(registerId)
    SetupStepCard(
        title = setup.stepApplication,
        hint = setup.stepApplicationHint,
        texts = setup,
        done = state.onRecord(registerId),
        ready = registerId != null && session.open,
        summary = listOfNotNull(setup.registered, record?.registrationNumber).joinToString(Glyphs.SEPARATOR)
    ) {
        val id = registerId ?: return@SetupStepCard
        if (state.onRecord(id)) return@SetupStepCard
        val texts = textsOf(LocalLanguage.current).cabinet
        RecordStatus(record, setup, texts)
        // Пока NCALayer ждёт подпись, на месте кнопок идёт отсчёт срока
        // с отменой — тот же, что на двери входа и при подаче из кабинета.
        if (state.signing) {
            cabinet.SignWait(actions::cancelSubmit)
        } else {
            SubmitButtons(setup, texts, session.busy, record?.awaiting == true, { actions.submit(id) }) {
                actions.readRecord(id)
            }
        }
    }
}

/**
 * Состояние кассы в кабинете словами.
 *
 * Состояние кассы — это ещё не состояние заявления: пока заявления
 * нет, «ждём ответа КГД» над черновиком просто врёт. Называется оно
 * словами: здесь стояло «Состояние кассы: DRAFT». Пока карточка
 * не прочитана, строки нет вовсе — на её месте повторялась
 * подсказка самого шага, уже написанная выше.
 */
@Composable
private fun RecordStatus(record: CabinetRecord?, setup: SetupTexts, cabinet: CabinetTexts) {
    val code = record?.status ?: return
    Text(
        text = "${setup.status} ${statusTitle(code, cabinet)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Подать заявление и перечитать кассу, не дожидаясь очередного круга.
 *
 * Пока заявление у КГД, второе подавать незачем: главное действие —
 * «Обновить», а подачи нет вовсе. Прежде «Подать заявление» оставалась
 * главной и живой, и владелец подавал то же заявление второй раз.
 *
 * @param awaiting заявление подано и ждёт ответа КГД.
 */
@Composable
private fun SubmitButtons(
    setup: SetupTexts,
    cabinet: CabinetTexts,
    busy: Boolean,
    awaiting: Boolean,
    onSubmit: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (awaiting) {
            BusyButton(text = cabinet.refresh, busy = busy, onClick = onRefresh)
        } else {
            BusyButton(text = setup.submit, busy = busy, onClick = onSubmit)
            TextButton(onClick = onRefresh) { Text(cabinet.refresh) }
        }
    }
}
