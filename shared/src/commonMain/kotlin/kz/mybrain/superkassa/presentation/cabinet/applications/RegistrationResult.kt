package kz.mybrain.superkassa.presentation.cabinet.applications

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.cabinetMessage
import kz.mybrain.superkassa.presentation.cabinet.register.noActionsReason
import kz.mybrain.superkassa.presentation.cabinet.statusTitle
import kz.mybrain.superkassa.presentation.common.section.DetailLine
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts

/*
 * Что владелец читает под кнопкой подачи: почему заявлений нет,
 * чем кончилась подача и короткие пояснения.
 */

/** Строка пояснения под кнопкой: её читают один раз и решают. */
@Composable
internal fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Почему заявлений сейчас нет — вместо ряда погашенных сегментов. */
@Composable
internal fun NoActions(register: CabinetRegister, texts: CabinetTexts) {
    Text(
        text = noActionsReason(register, texts),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Чем закончилась подача.
 *
 * Здесь стояли два кода протокола через точку — `SENT · KKM_INACTIVE`.
 * Ответ ИСНА приходит не сразу, и об этом сказано прямо: иначе состояние
 * «отправлено» читается как незавершённая работа приложения.
 */
@Composable
internal fun ApplicationResult(outcome: ApplicationOutcome?, texts: CabinetTexts) {
    when (outcome) {
        null -> Unit
        is ApplicationOutcome.Sent -> {
            DetailLine(texts.applicationSent, statusTitle(outcome.sent.actionStatus, texts))
            DetailLine(texts.registerStatus, outcome.sent.cashRegisterStatus?.let { statusTitle(it, texts) })
            Text(
                text = texts.applicationWait,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Неудача остаётся под кнопкой до следующей подачи: всплывающая строка
        // каркаса гаснет за секунды, и владелец не успевал прочитать причину.
        is ApplicationOutcome.Failed -> Text(
            text = "${texts.applicationFailed}: ${cabinetMessage(outcome.problem, texts).words()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
