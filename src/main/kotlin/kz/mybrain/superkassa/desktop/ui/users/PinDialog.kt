package kz.mybrain.superkassa.desktop.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.CashierTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Смена пина кассиру.
 *
 * Диалог закрывается только по ответу узла: на отказе кассир должен
 * видеть, что именно он ввёл, а не пустой список и погасшее окно.
 * Правило про свой пин сказано здесь же — это единственное место, где
 * оно применимо.
 */
@Composable
internal fun ChangePinDialog(
    money: MoneyTexts,
    who: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (String) -> Boolean
) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val problem = pinProblem(pin, money.cashiers, texts.users.forbiddenPin)

    // Диалог с одним полем открывается с курсором в нём: иначе кассир
    // набирает пин в пустоту и не понимает, почему кнопка не оживает.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(AppIcons.pin, contentDescription = null) },
        title = { Text(money.cashiers.changePinFor.format(who)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = UserRules.digitsOf(it) },
                    label = { Text(texts.users.newPin) },
                    singleLine = true,
                    isError = problem != null,
                    placeholder = { Text(money.cashiers.pinLength) },
                    supportingText = problem?.let { { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fieldWidth(texts.users.newPin, Sizes.fieldPin).focusRequester(focus)
                )
                Text(
                    text = money.cashiers.ownPin,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && UserRules.pinAccepted(pin),
                onClick = {
                    busy = true
                    scope.launch {
                        val done = onConfirm(pin)
                        busy = false
                        if (done) onDismiss()
                    }
                }
            ) { Text(if (busy) money.drawer.working else texts.users.change) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(money.drawer.cancel) }
        }
    )
}

/**
 * Что не так с набранным пином.
 *
 * Пустое поле ошибкой не считается: кассир ещё не начал вводить, и красное
 * поле встречало бы его до первого нажатия.
 */
internal fun pinProblem(pin: String, money: CashierTexts, forbidden: String): String? =
    when (UserRules.checkPin(pin)) {
        PinRefusal.TooShort -> money.pinTooShort
        PinRefusal.TooLong -> money.pinTooLong
        PinRefusal.Default -> forbidden
        null -> null
    }
