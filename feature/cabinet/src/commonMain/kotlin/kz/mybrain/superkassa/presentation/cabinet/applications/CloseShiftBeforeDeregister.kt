package kz.mybrain.superkassa.presentation.cabinet.applications

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
import kz.mybrain.superkassa.designsystem.dialog.FormDialog
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.domain.users.model.UserRules
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Закрытие смены перед снятием кассы с учёта.
 *
 * Кабинет отказывает снять кассу с открытой сменой — `SHIFT_IS_OPEN`,
 * и закрытая смена обязательна при любой причине, включая поломку
 * и утрату. Прежде владелец читал отказ и шёл закрывать смену окольным
 * путём: выйти из кабинета, войти в кассу, закрыть смену, вернуться.
 *
 * Спрашивается прямо здесь. Пин нужен потому, что закрытие смены —
 * фискальная операция кассы, и она спросит его всё равно; пин живёт только
 * в этом окне и на диск не попадает.
 *
 * Закрыть смену можно лишь у кассы, заведённой на этой машине: смену
 * чужой кассы здесь не видно, и обещать закрытие было бы обманом.
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
        title = texts.applications.shiftOpenTitle,
        icon = AppIcons.warning,
        action = texts.applications.closeShiftAndDeregister,
        close = texts.close,
        busy = busy,
        missing = listOfNotNull(texts.applications.adminPin.takeIf { UserRules.checkPin(pin) != null }),
        onDismiss = onDismiss,
        onAction = { onConfirm(pin) }
    ) {
        Text(texts.applications.shiftOpenAsk)
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = UserRules.digitsOf(it) },
            label = { Text(texts.applications.adminPin) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
