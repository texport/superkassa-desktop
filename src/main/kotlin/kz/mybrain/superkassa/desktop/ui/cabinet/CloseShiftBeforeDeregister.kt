package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.components.FormDialog
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.users.UserRules

/**
 * Закрытие смены перед снятием кассы с учёта.
 *
 * Кабинет отказывает снять кассу с открытой сменой — `SHIFT_IS_OPEN`,
 * и закрытая смена обязательна при любой причине, включая поломку
 * и утрату. Прежде владелец читал отказ и шёл закрывать смену окольным
 * путём: выйти из кабинета, войти в кассу, закрыть смену, вернуться.
 *
 * Спрашивается прямо здесь. Пин нужен потому, что закрытие смены —
 * фискальная операция узла, и он спросит его всё равно; пин живёт только
 * в этом окне и на диск не попадает.
 *
 * Закрыть смену можно лишь у кассы, заведённой на этой машине: смену
 * чужой кассы узел не видит, и обещать закрытие было бы обманом.
 */
@Composable
internal fun CloseShiftBeforeDeregister(
    texts: CabinetTexts,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    FormDialog(
        title = texts.shiftOpenTitle,
        icon = AppIcons.warning,
        action = texts.closeShiftAndDeregister,
        close = texts.close,
        busy = busy,
        missing = listOfNotNull(texts.adminPin.takeIf { UserRules.checkPin(pin) != null }),
        onDismiss = onDismiss,
        onAction = { onConfirm(pin) }
    ) {
        Text(texts.shiftOpenAsk)
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = UserRules.digitsOf(it) },
            label = { Text(texts.adminPin) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Касса, у которой можно закрыть смену отсюда.
 *
 * `null` означает, что предлагать нечего: касса заведена на другой
 * машине, и закрывать её смену надо там.
 */
internal fun closableHere(register: CabinetRegister, kkms: List<Kkm>): Kkm? =
    (nodeWork(register, kkms) as? NodeWork.Here)?.kkm

/** Отказал ли кабинет из-за открытой смены. */
internal fun CabinetProblem.isShiftOpen(): Boolean =
    this is CabinetProblem.Refused && code == SHIFT_IS_OPEN

/** Код, которым кабинет отвечает на действие при открытой смене. */
private const val SHIFT_IS_OPEN = "SHIFT_IS_OPEN"
