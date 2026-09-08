package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import kz.mybrain.superkassa.desktop.server.KkmInitRequest
import kz.mybrain.superkassa.desktop.server.initKkm
import kz.mybrain.superkassa.desktop.ui.components.EnvironmentPicker
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.ProviderPicker
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UserRules
import kz.mybrain.superkassa.desktop.ui.users.pinProblem

/**
 * Идентификатор и токен, выданные ОФД.
 *
 * Список ОФД и контуров приходит с узла: свой в приложении означал бы,
 * что нового ОФД владелец не увидит, пока не обновит программу.
 *
 * Своей карточки шаг не рисует — её ставит мастер: прежде он рисовал
 * заголовок снаружи, и рядом с карточкой первого шага это выглядело
 * как куски из разных экранов.
 */
@Composable
fun OfdStep(session: Session) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    val providers = session.dictionaries[Dictionary.OfdProviders].orEmpty()
    val environments = session.dictionaries[Dictionary.OfdEnvironments].orEmpty()
    var provider by remember { mutableStateOf("") }
    var environment by remember { mutableStateOf("") }
    var systemId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var adminPin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    // Пока владелец не выбрал сам, подставлено первое значение справочника.
    val chosenProvider = provider.ifEmpty { providers.firstOrNull()?.code.orEmpty() }
    val chosenEnvironment = environment.ifEmpty { environments.firstOrNull()?.code.orEmpty() }
    val cashiers = moneyTexts(session.language).cashiers
    val pinTrouble = pinProblem(adminPin, cashiers, texts.users.forbiddenPin)
    val filled = systemId.isNotBlank() && token.isNotBlank() &&
        chosenProvider.isNotEmpty() && chosenEnvironment.isNotEmpty() &&
        UserRules.pinAccepted(adminPin)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        itemVerticalAlignment = Alignment.Top
    ) {
        ProviderPicker(
            entries = providers,
            language = session.language.code,
            selectedCode = chosenProvider,
            onSelect = { provider = it }
        )
        EnvironmentPicker(
            entries = environments,
            language = session.language.code,
            selectedCode = chosenEnvironment,
            onSelect = { environment = it }
        )
        OutlinedTextField(
            value = systemId,
            onValueChange = { systemId = it.filter(Char::isDigit) },
            label = { Text(texts.settings.kkmIdentifier) },
            singleLine = true,
            modifier = Modifier.fieldWidth(texts.settings.kkmIdentifier, Sizes.fieldChoice)
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it.filter(Char::isDigit) },
            label = { Text(texts.settings.token) },
            singleLine = true,
            modifier = Modifier.fieldWidth(texts.settings.token, Sizes.fieldChoice)
        )
        OutlinedTextField(
            value = adminPin,
            onValueChange = { adminPin = UserRules.digitsOf(it) },
            label = { Text(texts.settings.adminPin) },
            singleLine = true,
            isError = pinTrouble != null,
            placeholder = { Text(cashiers.pinLength) },
            supportingText = pinTrouble?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fieldWidth(texts.settings.adminPin, Sizes.fieldChoice)
        )
        // Действие стоит в строке с полями и того же роста, что они.
        FieldButton(
            text = if (busy) texts.settings.registering else texts.settings.register,
            kind = FieldButtonKind.Filled,
            enabled = !busy && filled
        ) {
            busy = true
            scope.launch {
                val done = connect(session, chosenProvider, chosenEnvironment, systemId, token, adminPin)
                if (done) {
                    systemId = ""
                    token = ""
                    adminPin = ""
                }
                busy = false
            }
        }
    }
}

/** Заводит кассу на узле и объявляет, в каком она состоянии. */
@Suppress("LongParameterList")
private suspend fun connect(
    session: Session,
    provider: String,
    environment: String,
    systemId: String,
    token: String,
    adminPin: String
): Boolean {
    val texts = stringsOf(session.language).settings
    val request = KkmInitRequest(provider, environment, systemId, token, adminPin)
    val created = session.guard(texts.registerKkm) {
        session.client.initKkm(request, BOOTSTRAP_PIN)
    } ?: return false
    session.refreshKkms()
    session.report("${texts.registered}: ${session.titleOf(Dictionary.KkmStates, created.state)}")
    return true
}

/**
 * Пин, которым узел подтверждает право заводить кассы.
 *
 * Это не пин будущей кассы: её администратор получает тот, что набран
 * в поле рядом. Со стандартным касса рождалась бы недоступной — узел
 * не пускает по нему, а сменить его можно только войдя.
 */
private const val BOOTSTRAP_PIN = "0000"
