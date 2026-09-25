package kz.mybrain.superkassa.presentation.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.designsystem.field.fieldMinWidth
import kz.mybrain.superkassa.designsystem.picker.LabelledPicker
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.users.model.UserRules
import kz.mybrain.superkassa.presentation.words.users.pinProblem
import kz.mybrain.superkassa.strings.api.kassa.MoneyTexts

/**
 * Заведение кассира — главное действие экрана.
 *
 * Причина, по которой кнопка не нажимается, написана под тем полем,
 * которое её вызвало: касса откажет ровно по ней, а администратор заводит
 * кассира раз в полгода и не помнит наизусть, чем ей не угодит пин 1111.
 *
 * Поля — одно под другим во всю ширину карточки, как форма Material 3,
 * а «Добавить» — главная кнопка под ними у правого края. Прежде поля
 * переносились рядом с кнопкой как придётся: имя и роль по строке,
 * а пин делил строку с кнопкой.
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

/** Имя, роль и пин одно под другим и кнопка «Добавить» под ними. */
@Composable
private fun CashierFields(state: UsersUiState, actions: UsersActions, money: MoneyTexts) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        NameField(state.form, actions, money, Modifier.fillMaxWidth())
        RoleField(state, actions, Modifier.fillMaxWidth())
        PinField(state.form, actions, money, Modifier.fillMaxWidth())
        Button(
            onClick = actions::create,
            enabled = state.canCreate,
            modifier = Modifier.align(Alignment.End)
        ) { Text(LocalStrings.current.users.create) }
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
        label = { Text(texts.general.pin) },
        singleLine = true,
        isError = trouble != null,
        placeholder = { Text(money.cashiers.pinLength) },
        supportingText = trouble?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier.fieldMinWidth(texts.general.pin, Sizes.fieldPin)
    )
}
