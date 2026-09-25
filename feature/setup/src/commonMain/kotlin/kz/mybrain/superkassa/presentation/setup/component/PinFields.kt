package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.setup.KkmForm
import kz.mybrain.superkassa.presentation.words.users.pinProblem
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Пин администратора заводимой кассы.
 *
 * Причина, по которой пин не годится, написана под полем — как и в каждом
 * другом поле пина приложения: касса откажет ровно по ней, а «Завести
 * кассу» до этого просто не нажималась и о причине молчала.
 */
@Composable
internal fun AdminPinField(pin: String, onChange: (String) -> Unit) {
    val texts = LocalStrings.current
    val cashiers = textsOf(LocalLanguage.current).kassa.money.cashiers
    val trouble = pinProblem(pin, cashiers)
    OutlinedTextField(
        value = pin,
        onValueChange = onChange,
        label = { Text(texts.settingsScreen.adminPin) },
        singleLine = true,
        isError = trouble != null,
        placeholder = { Text(cashiers.pinLength) },
        supportingText = trouble?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = PIN_KEYS,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Пин администратора ещё раз.
 *
 * Стандартного пина у новой кассы нет: опечатка в единственном пине закрыла
 * бы кассу от владельца, поэтому пин набирается дважды, а расхождение
 * названо под полем повтора.
 */
@Composable
internal fun RepeatPinField(form: KkmForm, onChange: (String) -> Unit) {
    val setup = textsOf(LocalLanguage.current).setup
    OutlinedTextField(
        value = form.adminPinRepeat,
        onValueChange = onChange,
        label = { Text(setup.pinRepeat) },
        singleLine = true,
        isError = form.pinsDiffer,
        supportingText = if (form.pinsDiffer) {
            { Text(setup.pinsDiffer) }
        } else {
            null
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = PIN_KEYS,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Пин — только цифры: на телефоне и планшете открывается цифровая клавиатура. */
private val PIN_KEYS = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
