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
import kz.mybrain.superkassa.desktop.app.enrollKkm
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.KkmInitRequest
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.OfdChoice
import kz.mybrain.superkassa.desktop.ui.components.OfdTarget
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
    var target by remember { mutableStateOf(OfdTarget()) }
    var systemId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var adminPin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    // Пока владелец не выбрал сам, подставлен ОФД этого рабочего места —
    // тот, с которым работают его кассы. Прежде здесь стояло первое
    // значение справочника, то есть чужой ОФД.
    val own = workplaceOfd(session.kkms, providers, environments)
    val chosen = target.copy(
        provider = target.provider.ifEmpty { own.provider },
        environment = target.environment.ifEmpty { own.environment }
    )
    val cashiers = moneyTexts(session.language).cashiers
    val pinTrouble = pinProblem(adminPin, cashiers, texts.users.forbiddenPin)
    val filled = systemId.isNotBlank() && token.isNotBlank() && chosen.complete &&
        UserRules.pinAccepted(adminPin)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        itemVerticalAlignment = Alignment.Top
    ) {
        OfdChoice(chosen, providers, environments, session.language.code) { target = it }
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
                val done = connect(session, chosen, systemId, token, adminPin)
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

/** Заводит кассу на узле: ход общий с мастером и с кабинетом. */
private suspend fun connect(
    session: Session,
    target: OfdTarget,
    systemId: String,
    token: String,
    adminPin: String
): Boolean {
    val what = stringsOf(session.language).settings.registerKkm
    val request = KkmInitRequest(
        ofdId = target.provider,
        ofdEnvironment = target.environment,
        ofdSystemId = systemId,
        ofdToken = token,
        adminPin = adminPin
    )
    return session.enrollKkm(request, what) != null
}
