package kz.mybrain.superkassa.presentation.cabinet.signing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import kz.mybrain.superkassa.designsystem.dialog.FormDialog
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.domain.cabinet.model.signature.KeyProblem
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts

/**
 * Пароль к файлу ключа — окном формы Material 3.
 *
 * Сверху — какой файл выбран и кнопка «Другой файл», под ним — поле пароля
 * со скрытым вводом. Неудача прошлой попытки стоит под полем словами
 * владельца: окно не закрывается ради неё, и владелец поправляет пароль
 * или файл на месте.
 *
 * Пароль уходит подписывающему массивом знаков, и поле тут же очищается:
 * в разметке он не остаётся ни после подписи, ни после отмены.
 */
@Composable
internal fun KeyPasswordDialog(request: SignRequest.KeyPassword, eds: EdsTexts, onAnswer: (SignAnswer) -> Unit) {
    val password = rememberTextFieldState()
    val submit = { if (password.text.isNotEmpty()) onAnswer(SignAnswer.Password(password.take())) }
    FormDialog(
        title = eds.keyTitle,
        icon = AppIcons.signKeyFile,
        action = eds.keySign,
        close = eds.cancel,
        busy = false,
        missing = if (password.text.isEmpty()) listOf(eds.keyPassword) else emptyList(),
        onDismiss = {
            password.clearText()
            onAnswer(SignAnswer.Cancel)
        },
        onAction = submit
    ) {
        KeyFileLine(request.file, eds) { onAnswer(SignAnswer.OtherFile) }
        PasswordField(password, request.problem?.let { problemWords(it, eds) }, eds, submit)
    }
}

/** Выбранный файл и «Другой файл» — строкой списка Material 3. */
@Composable
private fun KeyFileLine(file: String, eds: EdsTexts, onOther: () -> Unit) {
    ListItem(
        headlineContent = { Text(file) },
        overlineContent = { Text(eds.keyFile) },
        leadingContent = { Icon(AppIcons.signKeyFile, contentDescription = null) },
        trailingContent = { TextButton(onClick = onOther) { Text(eds.keyOther) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * Поле пароля: скрытый ввод, фокус сразу в нём, «Готово» на клавиатуре подписывает.
 *
 * Глаз в конце поля показывает набранное: пароль ключа длинный, и владелец
 * не видел, где ошибся, пока подпись не отказывала.
 */
@Composable
private fun PasswordField(state: TextFieldState, problem: String?, eds: EdsTexts, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    OutlinedSecureTextField(
        state = state,
        label = { Text(eds.keyPassword) },
        textObfuscationMode = if (shown) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
        trailingIcon = {
            IconButton(onClick = { shown = !shown }) {
                Icon(
                    imageVector = if (shown) AppIcons.hideSecret else AppIcons.preview,
                    contentDescription = if (shown) eds.hidePassword else eds.showPassword
                )
            }
        },
        isError = problem != null,
        supportingText = problem?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { onDone() },
        modifier = Modifier.fillMaxWidth().focusRequester(focus)
    )
}

/** Пароль массивом знаков; поле очищается. */
private fun TextFieldState.take(): CharArray {
    val chars = CharArray(text.length) { text[it] }
    clearText()
    return chars
}

/** Неудача прошлой попытки словами владельца. */
private fun problemWords(problem: KeyProblem, eds: EdsTexts): String = when (problem) {
    KeyProblem.WrongPassword -> eds.wrongPassword
    KeyProblem.Unreadable -> eds.keyUnreadable
    KeyProblem.NotForSigning -> eds.keyNotForSigning
    KeyProblem.Expired -> eds.keyExpired
}
