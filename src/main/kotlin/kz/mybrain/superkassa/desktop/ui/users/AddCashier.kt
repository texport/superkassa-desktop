package kz.mybrain.superkassa.desktop.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.KkmUserRequest
import kz.mybrain.superkassa.desktop.server.addUser
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.RolePicker
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Заведение кассира — главное действие экрана.
 *
 * Причина, по которой кнопка не нажимается, написана под тем полем,
 * которое её вызвало: узел откажет ровно по ней, а администратор заводит
 * кассира раз в полгода и не помнит наизусть, чем ему не угодит пин 1111.
 *
 * Поля разложены переносом, а не в одну строку: в узком окне строка из
 * трёх полей и кнопки обрезается, и первым уезжает пин.
 */
@Composable
internal fun AddCashier(session: Session, money: MoneyTexts, onCreated: suspend () -> Unit) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(DEFAULT_ROLE) }

    val cashiers = money.cashiers
    val pinTrouble = pinProblem(pin, cashiers, texts.users.forbiddenPin)
    val nameMissing = name.isBlank() && pin.isNotEmpty()

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cashiers.addTitle, style = MaterialTheme.typography.titleMedium)
                InfoTip(cashiers.roles)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight),
                itemVerticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(texts.users.name) },
                    singleLine = true,
                    isError = nameMissing,
                    supportingText = if (nameMissing) {
                        { Text(cashiers.nameRequired) }
                    } else {
                        null
                    },
                    modifier = Modifier.fieldWidth(texts.users.name, Sizes.fieldForm)
                )
                RolePicker(
                    entries = session.dictionaries[Dictionary.UserRoles].orEmpty(),
                    language = session.language.code,
                    selectedCode = role,
                    onSelect = { role = it }
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = UserRules.digitsOf(it) },
                    label = { Text(texts.common.pin) },
                    singleLine = true,
                    isError = pinTrouble != null,
                    placeholder = { Text(cashiers.pinLength) },
                    supportingText = pinTrouble?.let { { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fieldWidth(texts.common.pin, Sizes.fieldPin)
                )
                FieldButton(
                    text = texts.users.create,
                    kind = FieldButtonKind.Filled,
                    enabled = session.selected != null && UserRules.canCreate(name, pin)
                ) {
                    scope.launch {
                        val created = name.trim()
                        if (create(session, texts.users.create, created, role, pin)) {
                            name = ""
                            pin = ""
                            // Список перечитывается до сообщения: он снимает
                            // предыдущее, и объяви мы итог раньше — кассир
                            // остался бы без подтверждения.
                            onCreated()
                            // Итог согласован с названием роли, а не с именем:
                            // «заведён» рядом с женским именем звучит ошибкой,
                            // и род человека тут вообще ни при чём.
                            val who = session.titleOf(Dictionary.UserRoles, role)
                            session.report("$who ${texts.users.created}: $created")
                        }
                    }
                }
            }
            // Про несовпадение пинов сказано тогда, когда пин уже набран:
            // до этого правило ничего не объясняет и просто занимает строку.
            if (UserRules.pinAccepted(pin)) {
                Text(
                    text = cashiers.pinUnique,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Заводит кассира на узле. Об итоге объявляет вызывающий. */
private suspend fun create(
    session: Session,
    what: String,
    name: String,
    role: String,
    pin: String
): Boolean {
    val kkm = session.selected ?: return false
    session.guard(what) {
        session.client.addUser(kkm.kkmId, KkmUserRequest(name, role, pin), session.pin)
    } ?: return false
    return true
}

/** Роль по умолчанию для нового пользователя. */
const val DEFAULT_ROLE = "CASHIER"
