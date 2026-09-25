package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.button.FieldButtonKind
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.presentation.setup.SetupOffice
import kz.mybrain.superkassa.presentation.setup.component.StepDone
import kz.mybrain.superkassa.presentation.words.cabinet.statusTitle
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаг постановки кассы на учёт в КГД.
 *
 * Заявление готовит кабинет, подписывает владелец ключом ЭЦП, отправляет
 * снова кабинет. Ответ КГД приходит не в ту же минуту, поэтому шаг
 * не притворяется сделанным: он показывает состояние кассы в кабинете,
 * а мастер перечитывает его сам, пока номера ещё нет. «Обновить» остаётся —
 * им спрашивают, не дожидаясь очередного круга.
 *
 * @param registerId касса в кабинете, которую ставят на учёт.
 */
@Composable
internal fun ApplicationStep(office: SetupOffice, registerId: String?) {
    val id = registerId ?: return
    val state = office.registration
    val record = state.recordOf(id)
    val setup = textsOf(LocalLanguage.current).setup
    when {
        state.onRecord(id) -> StepDone(setup.registered, record?.registrationNumber, null, setup.done)
        !office.session.open -> SignInFirst(office.cabinet)
        // Пока владелец подписывает, на месте кнопок идёт отсчёт срока
        // с отменой — тот же, что на двери входа и при подаче из кабинета.
        state.signing -> office.cabinet.SignWait(office.actions::cancelSubmit)
        else -> {
            RecordStatus(record)
            SubmitButtons(office, id, awaiting = record?.awaiting == true)
        }
    }
}

/**
 * Состояние кассы в кабинете словами.
 *
 * Пока карточка не прочитана, строки нет вовсе: «ждём ответа КГД» над
 * черновиком, которого ещё не подавали, просто врёт.
 */
@Composable
private fun RecordStatus(record: CabinetRecord?) {
    val code = record?.status ?: return
    val language = LocalLanguage.current
    Text(
        text = "${textsOf(language).setup.status} ${statusTitle(code, textsOf(language).cabinet)}",
        style = MaterialTheme.typography.bodyLarge
    )
}

/**
 * Подать заявление и перечитать кассу, не дожидаясь очередного круга.
 *
 * Пока заявление у КГД, второе подавать незачем: подачи нет вовсе,
 * остаётся «Обновить». Кнопки шага тональные: главное действие — «Далее».
 *
 * @param awaiting заявление подано и ждёт ответа КГД.
 */
@Composable
private fun SubmitButtons(office: SetupOffice, id: String, awaiting: Boolean) {
    val language = LocalLanguage.current
    val refresh = textsOf(language).cabinet.refresh
    val busy = office.session.busy
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (awaiting) {
            BusyButton(text = refresh, busy = busy, kind = FieldButtonKind.Tonal) { office.actions.readRecord(id) }
        } else {
            val submit = textsOf(language).setup.submit
            BusyButton(text = submit, busy = busy, kind = FieldButtonKind.Tonal) { office.actions.submit(id) }
            TextButton(onClick = { office.actions.readRecord(id) }) { Text(refresh) }
        }
    }
}
