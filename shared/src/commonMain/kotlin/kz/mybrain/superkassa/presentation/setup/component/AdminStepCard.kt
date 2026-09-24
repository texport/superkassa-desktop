package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.setup.KkmForm
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupUiState
import kz.mybrain.superkassa.presentation.words.users.pinProblem
import kz.mybrain.superkassa.strings.api.setup.SetupTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаг 4: касса заводится на рабочем месте, у неё появляется администратор.
 *
 * Токен выпускается кабинетом в момент нажатия и в файл не попадает:
 * это ключ, которым касса подписывает запросы. Владелец его не видит
 * и не переписывает — он идёт из кабинета в кассу внутри одного действия.
 *
 * Пин администратора задаётся здесь же: пинов по умолчанию у кассы нет,
 * и без него касса не заводится. Шаг открывается, когда касса в кабинете
 * встала на учёт: токен выпускается только ей.
 *
 * @param onRecord касса в кабинете встала на учёт: токен выпускается только ей.
 * @param cabinetBusy кабинет окна занят обращением: кнопка ждёт вместе с ним.
 * @param onDone касса заведена и читается: мастер своё сделал.
 */
@Composable
fun AdminStepCard(
    state: SetupUiState,
    actions: SetupActions,
    setup: SetupTexts,
    onRecord: Boolean,
    cabinetBusy: Boolean,
    onDone: () -> Unit
) {
    SetupStepCard(
        title = setup.stepAdmin,
        hint = setup.stepAdminHint,
        texts = setup,
        done = state.connected,
        ready = onRecord,
        summary = setup.connected
    ) {
        if (!state.connected) AdminForm(state, actions, setup, cabinetBusy, onDone)
    }
}

/** Контур, пин администратора и заведение. */
@Composable
private fun AdminForm(
    state: SetupUiState,
    actions: SetupActions,
    setup: SetupTexts,
    cabinetBusy: Boolean,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        ContourPicker(state.contours, state.contour) { actions.edit(state.form.copy(contour = it)) }
        AdminPinField(state.form.adminPin) { actions.edit(state.form.copy(adminPin = it)) }
        RepeatPinField(state.form) { actions.edit(state.form.copy(adminPinRepeat = it)) }
        BusyButton(
            text = setup.connect,
            busy = state.form.busy || cabinetBusy,
            enabled = state.ready,
            onClick = { actions.connect(onDone) }
        )
    }
}

/**
 * Пин администратора заводимой кассы.
 *
 * Причина, по которой пин не годится, написана под полем — как и в каждом
 * другом поле пина приложения: касса откажет ровно по ней, а «Завести
 * кассу» до этого просто не нажималась и о причине молчала.
 */
@Composable
internal fun AdminPinField(pin: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    val texts = LocalStrings.current
    val cashiers = textsOf(LocalLanguage.current).kassa.money.cashiers
    val trouble = pinProblem(pin, cashiers)
    OutlinedTextField(
        value = pin,
        onValueChange = onChange,
        label = { Text(texts.settings.adminPin) },
        singleLine = true,
        isError = trouble != null,
        placeholder = { Text(cashiers.pinLength) },
        supportingText = trouble?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier
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
internal fun RepeatPinField(form: KkmForm, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
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
        modifier = modifier
    )
}
