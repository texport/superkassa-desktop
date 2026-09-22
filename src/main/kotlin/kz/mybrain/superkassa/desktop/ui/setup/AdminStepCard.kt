package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.enrollKkm
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.KkmInitRequest
import kz.mybrain.superkassa.desktop.server.cabinet.issueToken
import kz.mybrain.superkassa.desktop.server.cabinet.register
import kz.mybrain.superkassa.desktop.ui.components.BFD_PROVIDER
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.EnvironmentPicker
import kz.mybrain.superkassa.desktop.ui.components.environmentRaised
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UserRules
import kz.mybrain.superkassa.desktop.ui.users.pinProblem

/**
 * Шаг 4: касса заводится в узле, у неё появляется администратор.
 *
 * Токен запрашивается у кабинета в момент нажатия и в файл не попадает:
 * это ключ, которым касса подписывает запросы. Владелец его не видит
 * и не переписывает — он идёт из кабинета в узел внутри одного действия.
 *
 * Пин администратора задаётся здесь же: без него в заведённую кассу
 * не войти, а стандартный пин узел не принимает.
 */
@Composable
fun AdminStepCard(
    session: Session,
    cabinet: CabinetSession,
    setup: SetupTexts,
    draft: KkmSetupDraft,
    registered: Boolean,
    onDone: () -> Unit
) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    val environments = session.dictionaries[Dictionary.OfdEnvironments].orEmpty()
    var environment by remember { mutableStateOf("") }
    val chosenEnvironment = environment.ifEmpty {
        environments.firstOrNull { it.environmentRaised() }?.code.orEmpty()
    }
    var onRecord by remember(draft.cabinetRegisterId) { mutableStateOf(false) }

    // Состояние перечитывается и когда касса встала на учёт: прежде шаг
    // спрашивал его один раз при открытии мастера и оставался в «ждёт
    // предыдущего шага» до тех пор, пока владелец не откроет мастер заново.
    LaunchedEffect(draft.cabinetRegisterId, cabinet.token, registered) {
        val token = cabinet.token ?: return@LaunchedEffect
        val id = draft.cabinetRegisterId ?: return@LaunchedEffect
        onRecord = cabinet.guard { cabinet.client.register(token, id) }
            ?.registrationNumber?.isNotBlank() == true
    }

    // Сверка по идентификатору идёт только тогда, когда он есть: без
    // черновика идентификатор пуст, и «пусто равно пусто» помечало шаг
    // пройденным у любого, у кого на узле есть касса без сведений об ОФД.
    // Нетронутый мастер встречал владельца готовым «Касса подключена».
    val already = draft.systemId?.let { known -> session.kkms.any { it.ofdSystemId == known } } == true
    SetupStepCard(
        title = setup.stepAdmin,
        hint = setup.stepAdminHint,
        texts = setup,
        done = already,
        ready = onRecord,
        summary = setup.connected
    ) {
        if (already) return@SetupStepCard
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            EnvironmentPicker(
                entries = environments,
                language = session.language.code,
                selectedCode = chosenEnvironment,
                onSelect = { environment = it }
            )
            AdminPinField(session, pin) { pin = it }
            BusyButton(
                text = setup.connect,
                busy = session.busy || cabinet.busy,
                enabled = UserRules.pinAccepted(pin),
                onClick = {
                    scope.launch {
                        val what = setup.stepAdmin
                        if (connect(session, cabinet, draft, Registration(chosenEnvironment, pin), what)) {
                            draft.clear()
                            onDone()
                        }
                    }
                }
            )
        }
    }
}

/**
 * Пин администратора заводимой кассы.
 *
 * Причина, по которой пин не годится, написана под полем — как и в каждом
 * другом поле пина приложения: узел откажет ровно по ней, а «Завести
 * кассу» до этого просто не нажималась и о причине молчала.
 */
@Composable
private fun AdminPinField(session: Session, pin: String, onChange: (String) -> Unit) {
    val texts = LocalStrings.current
    val cashiers = moneyTexts(session.language).cashiers
    val trouble = pinProblem(pin, cashiers, texts.users.forbiddenPin)
    OutlinedTextField(
        value = pin,
        onValueChange = { onChange(UserRules.digitsOf(it)) },
        label = { Text(texts.settings.adminPin) },
        singleLine = true,
        isError = trouble != null,
        placeholder = { Text(cashiers.pinLength) },
        supportingText = trouble?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Выдаёт токен и заводит кассу в узле одним действием.
 *
 * Между выдачей токена и заведением кассы токен нигде не задерживается:
 * ни на экране, ни в настройках рабочего места. Само заведение — общий
 * ход [enrollKkm]: он же зовётся из настроек БФД и из паспорта кассы.
 */
private suspend fun connect(
    session: Session,
    cabinet: CabinetSession,
    draft: KkmSetupDraft,
    registration: Registration,
    what: String
): Boolean {
    val cabinetToken = cabinet.token ?: return false
    val registerId = draft.cabinetRegisterId ?: return false
    val systemId = draft.systemId ?: return false
    val issued = cabinet.guard { cabinet.client.issueToken(cabinetToken, registerId) } ?: return false
    val request = KkmInitRequest(
        ofdId = BFD_PROVIDER,
        ofdEnvironment = registration.environment,
        ofdSystemId = systemId,
        ofdToken = issued.token.toString(),
        adminPin = registration.adminPin
    )
    return session.enrollKkm(request, what, draft.name) != null
}

/** Чем и от чьего имени касса заводится в узле. */
private data class Registration(val environment: String, val adminPin: String)
