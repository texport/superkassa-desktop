package kz.mybrain.superkassa.presentation.users

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kz.mybrain.superkassa.presentation.common.dialog.DialogBody
import kz.mybrain.superkassa.presentation.common.dialog.DialogTitle
import kz.mybrain.superkassa.presentation.common.format.fill
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.MoneyTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Смена пина кассиру.
 *
 * Окно закрывается только по ответу кассы: на отказе кассир должен
 * видеть, что именно он ввёл и почему не вышло — «пин занят» и «касса
 * заперта» стоят под полем словами кассы. Правило про свой пин сказано
 * здесь же — и только тому, кто меняет пин себе.
 *
 * @param roleTitle как назвать кассира, если имени у него нет.
 */
@Composable
internal fun ChangePinDialog(money: MoneyTexts, change: PinChange, roleTitle: String, actions: UsersActions) {
    val texts = LocalStrings.current
    AlertDialog(
        onDismissRequest = { if (!change.busy) actions.askPin(null) },
        icon = { Icon(AppIcons.pin, contentDescription = null) },
        title = { DialogTitle(money.cashiers.changePinFor.fill(change.user.name.ifBlank { roleTitle })) },
        text = { NewPinField(money, change, actions) },
        confirmButton = {
            Button(enabled = change.ready, onClick = actions::confirmPin) {
                Text(if (change.busy) money.drawer.working else texts.users.change)
            }
        },
        dismissButton = {
            TextButton(enabled = !change.busy, onClick = { actions.askPin(null) }) { Text(money.drawer.cancel) }
        }
    )
}

/** Поле нового пина, причина под ним и — только своему — обещание продолжить работу. */
@Composable
private fun NewPinField(money: MoneyTexts, change: PinChange, actions: UsersActions) {
    val texts = LocalStrings.current
    val problem = pinProblem(change.pin, money.cashiers) ?: change.refusal
    // Диалог с одним полем открывается с курсором в нём: иначе кассир
    // набирает пин в пустоту и не понимает, почему кнопка не оживает.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    DialogBody(spacing = Spacing.tight) {
        OutlinedTextField(
            value = change.pin,
            onValueChange = actions::typeNewPin,
            label = { Text(texts.users.newPin) },
            singleLine = true,
            isError = problem != null,
            placeholder = { Text(money.cashiers.pinLength) },
            supportingText = problem?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            // Поле во всю ширину диалога: причина под ним занимает его
            // ширину, и в поле ширины пина «ПИН-де төрт цифрдан аз»
            // ломалось на строки по слову.
            modifier = Modifier.fillMaxWidth().focusRequester(focus)
        )
        // Обещание продолжить работу новым пином — только тому, кто меняет
        // пин себе. Над чужим кассиром оно обещало администратору работу
        // под чужим пином.
        if (change.own) OwnPinNote(money.cashiers.ownPin)
    }
}

@Composable
private fun OwnPinNote(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
