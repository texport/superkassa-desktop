package kz.mybrain.superkassa.presentation.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.domain.users.model.UserRules
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.common.button.FieldButtonKind
import kz.mybrain.superkassa.presentation.common.field.fieldMinWidth
import kz.mybrain.superkassa.presentation.common.picker.LabelledPicker
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.MoneyTexts
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Заведение кассира — главное действие экрана.
 *
 * Причина, по которой кнопка не нажимается, написана под тем полем,
 * которое её вызвало: касса откажет ровно по ней, а администратор заводит
 * кассира раз в полгода и не помнит наизусть, чем ей не угодит пин 1111.
 *
 * Поля разложены переносом, а не в одну строку: в узком окне строка из
 * трёх полей и кнопки обрезается, и первым уезжает пин. Поля тянутся до
 * конца строки: в широкой карточке поля заданной ширины оставляли справа
 * от себя пустую половину.
 */
@Composable
internal fun AddCashier(state: UsersUiState, actions: UsersActions, money: MoneyTexts) {
    val form = state.form
    val cashiers = money.cashiers
    SectionCard(title = cashiers.addTitle, info = cashiers.roles) {
        CashierFields(state, actions, money)
        // Про несовпадение пинов сказано тогда, когда пин уже набран:
        // до этого правило ничего не объясняет и просто занимает строку.
        if (UserRules.pinAccepted(form.pin)) {
            Text(
                text = cashiers.pinUnique,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Имя, роль и пин в переносимом ряду и кнопка заведения в его конце. */
@Composable
private fun CashierFields(state: UsersUiState, actions: UsersActions, money: MoneyTexts) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        itemVerticalAlignment = Alignment.Top
    ) {
        NameField(state.form, actions, money, Modifier.weight(1f))
        RoleField(state, actions, Modifier.weight(1f).widthIn(min = Sizes.fieldAmount))
        PinField(state.form, actions, money, Modifier.weight(1f))
        val create = LocalStrings.current.users.create
        FieldButton(create, FieldButtonKind.Filled, state.canCreate, onClick = actions::create)
    }
}

@Composable
private fun NameField(form: CashierForm, actions: UsersActions, money: MoneyTexts, modifier: Modifier) {
    val label = LocalStrings.current.users.name
    val missing = form.name.isBlank() && form.pin.isNotEmpty()
    OutlinedTextField(
        value = form.name,
        onValueChange = { actions.edit(form.copy(name = it)) },
        label = { Text(label) },
        singleLine = true,
        isError = missing,
        supportingText = if (missing) {
            { Text(money.cashiers.nameRequired) }
        } else {
            null
        },
        modifier = modifier.fieldMinWidth(label, Sizes.fieldForm)
    )
}

/**
 * Роль: названия — со слов кассы, пока она молчит — свои.
 *
 * Без своих слов в поле стоял бы код «CASHIER» латиницей.
 */
@Composable
private fun RoleField(state: UsersUiState, actions: UsersActions, modifier: Modifier) {
    val texts = LocalStrings.current.users
    val language = LocalLanguage.current
    LabelledPicker(
        label = texts.role,
        options = UserRole.entries,
        selected = state.form.role,
        title = { role -> role?.let { roleWord(it, texts, state.roleNames, language) }.orEmpty() },
        onSelect = { actions.edit(state.form.copy(role = it)) },
        modifier = modifier
    )
}

@Composable
private fun PinField(form: CashierForm, actions: UsersActions, money: MoneyTexts, modifier: Modifier) {
    val texts = LocalStrings.current
    val trouble = pinProblem(form.pin, money.cashiers)
    OutlinedTextField(
        value = form.pin,
        onValueChange = { actions.edit(form.copy(pin = UserRules.digitsOf(it))) },
        label = { Text(texts.common.pin) },
        singleLine = true,
        isError = trouble != null,
        placeholder = { Text(money.cashiers.pinLength) },
        supportingText = trouble?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier.fieldMinWidth(texts.common.pin, Sizes.fieldPin)
    )
}
