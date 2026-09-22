package kz.mybrain.superkassa.desktop.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
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
import kz.mybrain.superkassa.desktop.app.ADMIN_ROLE
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.KkmUserRequest
import kz.mybrain.superkassa.desktop.server.addUser
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.RolePicker
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.UserStrings
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

    SectionCard(title = cashiers.addTitle, info = cashiers.roles) {
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
                entries = roleEntries(session, texts.users),
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
                        val who = roleTitle(session, texts.users, role)
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

/**
 * Роли для выбора: от узла, а своими названиями — пока он молчит.
 *
 * Без запасного набора в поле стоял протокольный код «CASHIER» латиницей:
 * справочник ролей приходит отдельным обращением, и при недоступном узле
 * владелец читал в поле не слово, а код.
 */
private fun roleEntries(session: Session, texts: UserStrings): List<DictionaryEntry> {
    val language = session.language.code
    return session.dictionaries[Dictionary.UserRoles].orEmpty().ifEmpty {
        ROLES.map { code -> DictionaryEntry(code, mapOf(language to (ownTitle(code, texts) ?: code))) }
    }
}

/**
 * Название роли на языке кассира.
 *
 * Сначала справочник узла, потом своё слово: справочник приходит отдельным
 * обращением, и до ответа в строке кассира стояло «ADMIN» латиницей.
 */
internal fun roleTitle(session: Session, texts: UserStrings, code: String?): String {
    val fromNode = session.titleOf(Dictionary.UserRoles, code)
    if (code == null || fromNode != code) return fromNode
    return ownTitle(code, texts) ?: code
}

/** Своё название известной роли; чужую роль назвать нечем. */
private fun ownTitle(code: String, texts: UserStrings): String? = when (code) {
    ADMIN_ROLE -> texts.admin
    DEFAULT_ROLE -> texts.cashier
    else -> null
}

/** Роли, которые касса знает сама: узел называет их теми же кодами. */
private val ROLES = listOf(ADMIN_ROLE, DEFAULT_ROLE)

/** Роль по умолчанию для нового пользователя. */
const val DEFAULT_ROLE = "CASHIER"
