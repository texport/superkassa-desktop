package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.button.FieldButton
import kz.mybrain.superkassa.designsystem.button.FieldButtonKind
import kz.mybrain.superkassa.designsystem.button.underFieldLabel
import kz.mybrain.superkassa.designsystem.field.fieldWidth
import kz.mybrain.superkassa.designsystem.keyboard.onEnter
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.signin.model.Pin

/**
 * Пин и вход.
 *
 * Отдельная поверхность с тональной подложкой: по Material 3 действие,
 * закреплённое у нижнего края, отделяется от прокручиваемого списка
 * не отступом, а собственным уровнем поверхности.
 *
 * На телефоне — колонкой: касса над пином, пин рядом со входом. В одну
 * строку поле пина, название кассы, «Обновить» и «Войти» на 360 dp
 * не помещались, и название кассы сжималось до буквы — а это первый
 * экран телефона.
 */
@Composable
internal fun SignInBar(state: LoginUiState, actions: LoginActions) {
    val ready = state.chosen != null && Pin.enterable(state.pin)
    // Кассир набирает пин и жмёт Enter, не тянясь к мыши: на настольной
    // кассе клавиатурное действие поля важнее, чем в мобильном Material,
    // и одним `imeAction` его не получить.
    val submit = { ready.also { if (it) actions.enter() } }
    // Блок пина стоит в тех же полях, что и список: разъехавшиеся по
    // ширине карточки на одном экране читаются как разные разделы.
    Surface(
        tonalElevation = Sizes.barElevation,
        shape = RoundedCornerShape(Sizes.largeCorner),
        modifier = Modifier.fillMaxWidth()
    ) {
        val padded = Modifier.fillMaxWidth().padding(Spacing.blockPadding).onEnter { submit() }
        if (LocalWindowClass.current.width == WidthClass.Compact) {
            StackedBar(state, actions, ready, padded) { submit() }
        } else {
            WideBar(state, actions, ready, padded) { submit() }
        }
    }
}

/** Всё одной строкой: пин, касса, «Обновить», «Войти» — окно шире телефона. */
@Composable
private fun WideBar(
    state: LoginUiState,
    actions: LoginActions,
    ready: Boolean,
    modifier: Modifier,
    onGo: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PinField(state.pin, actions, Modifier.fieldWidth(LocalStrings.current.common.pin, Sizes.fieldPin), onGo)
        ChosenKkm(state, modifier = Modifier.weight(1f).underFieldLabel())
        ReloadButton(actions, Modifier.underFieldLabel())
        EnterButton(ready, actions)
    }
}

/** Телефон: касса с «Обновить» строкой выше, пин во всю ширину рядом со входом. */
@Composable
private fun StackedBar(
    state: LoginUiState,
    actions: LoginActions,
    ready: Boolean,
    modifier: Modifier,
    onGo: () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChosenKkm(state, modifier = Modifier.weight(1f))
            ReloadButton(actions)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PinField(state.pin, actions, Modifier.weight(1f), onGo)
            EnterButton(ready, actions)
        }
    }
}

@Composable
private fun ReloadButton(actions: LoginActions, modifier: Modifier = Modifier) {
    IconButton(onClick = actions::reload, modifier = modifier) {
        Icon(AppIcons.refresh, contentDescription = LocalStrings.current.login.reload)
    }
}

@Composable
private fun EnterButton(ready: Boolean, actions: LoginActions, modifier: Modifier = Modifier) {
    FieldButton(
        LocalStrings.current.login.enter,
        FieldButtonKind.Filled,
        enabled = ready,
        modifier = modifier,
        onClick = actions::enter
    )
}

/** Поле пина: цифры, скрыты, «Готово» клавиатуры входит. */
@Composable
private fun PinField(pin: String, actions: LoginActions, modifier: Modifier, onGo: () -> Unit) {
    val texts = LocalStrings.current
    OutlinedTextField(
        value = pin,
        onValueChange = actions::typePin,
        label = { Text(texts.common.pin) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = modifier
    )
}

/**
 * Какая касса откроется этим пином.
 *
 * Название первой строкой, регистрационный номер — второй. Прежде здесь
 * стоял один номер, и кассир, назвавший кассу по-своему, не узнавал её
 * в строке над кнопкой входа.
 */
@Composable
private fun ChosenKkm(state: LoginUiState, modifier: Modifier = Modifier) {
    val texts = LocalStrings.current
    val chosen = state.chosen
    Column(modifier = modifier) {
        Text(
            // Одно название без слова «Касса» перед ним: у кассы,
            // названной «Касса у входа», получалось «Касса Касса у входа».
            text = chosen?.let(state::nameOf) ?: texts.login.noKkmChosen,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = chosen?.let { kkmNumber(it, texts.login) } ?: texts.login.pickHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
