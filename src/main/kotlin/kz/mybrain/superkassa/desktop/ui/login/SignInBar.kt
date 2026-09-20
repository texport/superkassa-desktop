package kz.mybrain.superkassa.desktop.ui.login

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
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.components.onEnter
import kz.mybrain.superkassa.desktop.ui.components.underFieldLabel
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UserRules

/**
 * Пин и вход.
 *
 * Отдельная поверхность с тональной подложкой: по Material 3 действие,
 * закреплённое у нижнего края, отделяется от прокручиваемого списка
 * не отступом, а собственным уровнем поверхности.
 */
@Composable
internal fun SignInBar(
    pin: String,
    nameOf: (Kkm) -> String,
    onPin: (String) -> Unit,
    chosen: Kkm?,
    onEnter: () -> Unit,
    onReload: () -> Unit
) {
    val texts = LocalStrings.current
    val ready = chosen != null && UserRules.pinEnterable(pin)

    // Кассир набирает пин и жмёт Enter, не тянясь к мыши: на настольной
    // кассе клавиатурное действие поля важнее, чем в мобильном Material,
    // и одним `imeAction` его не получить.
    val submit = {
        if (ready) {
            onEnter()
            true
        } else {
            false
        }
    }
    // Блок пина стоит в тех же полях, что и список: разъехавшиеся по
    // ширине карточки на одном экране читаются как разные разделы.
    Surface(
        tonalElevation = Sizes.barElevation,
        shape = RoundedCornerShape(Sizes.largeCorner),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy).onEnter { submit() },
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = onPin,
                label = { Text(texts.common.pin) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                modifier = Modifier.fieldWidth(texts.common.pin, Sizes.fieldPin)
            )
            ChosenKkm(chosen, nameOf, modifier = Modifier.weight(1f).underFieldLabel())
            IconButton(onClick = onReload, modifier = Modifier.underFieldLabel()) {
                Icon(AppIcons.refresh, contentDescription = texts.login.reload)
            }
            FieldButton(texts.login.enter, FieldButtonKind.Filled, enabled = ready, onClick = onEnter)
        }
    }
}

/**
 * Какая касса откроется этим пином.
 *
 * Название первой строкой, регистрационный номер — второй. Прежде здесь
 * стоял один номер, и кассир, назвавший кассу по-своему, не узнавал её
 * в строке над кнопкой входа.
 */
@Composable
private fun ChosenKkm(chosen: Kkm?, nameOf: (Kkm) -> String, modifier: Modifier = Modifier) {
    val texts = LocalStrings.current
    Column(modifier = modifier) {
        Text(
            // Одно название без слова «Касса» перед ним: у кассы,
            // названной «Касса у входа», получалось «Касса Касса у входа».
            text = chosen?.let(nameOf) ?: texts.login.noKkmChosen,
            style = MaterialTheme.typography.bodyMedium
        )
        val under = chosen?.let { kkmNumber(it, texts.login) } ?: texts.login.pickHint
        if (under != null) {
            Text(
                text = under,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
