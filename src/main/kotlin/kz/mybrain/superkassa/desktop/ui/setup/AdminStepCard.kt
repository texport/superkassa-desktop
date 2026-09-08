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
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.KkmInitRequest
import kz.mybrain.superkassa.desktop.server.cabinet.issueToken
import kz.mybrain.superkassa.desktop.server.cabinet.register
import kz.mybrain.superkassa.desktop.server.initKkm
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.EnvironmentPicker
import kz.mybrain.superkassa.desktop.ui.components.ProviderPicker
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UserRules

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
    val providers = session.dictionaries[Dictionary.OfdProviders].orEmpty()
    val environments = session.dictionaries[Dictionary.OfdEnvironments].orEmpty()
    var provider by remember { mutableStateOf("") }
    var environment by remember { mutableStateOf("") }
    // Касса заведена в кабинете БФД, значит и данные она шлёт БФД:
    // первый из справочника подставлял чужого оператора, и владелец
    // узнавал об этом отказом уже после нажатия.
    val chosenProvider = provider.ifEmpty {
        providers.firstOrNull { it.code == CABINET_PROVIDER }?.code
            ?: providers.firstOrNull()?.code.orEmpty()
    }
    val chosenEnvironment = environment.ifEmpty { environments.firstOrNull()?.code.orEmpty() }
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

    val already = session.kkms.any { it.ofdSystemId == draft.systemId }
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
                value = pin,
                onValueChange = { pin = UserRules.digitsOf(it) },
                label = { Text(texts.settings.adminPin) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            BusyButton(
                text = setup.connect,
                busy = session.busy || cabinet.busy,
                enabled = UserRules.pinAccepted(pin),
                onClick = {
                    scope.launch {
                        val what = setup.stepAdmin
                        if (connect(session, cabinet, draft, Registration(chosenProvider, chosenEnvironment, pin), what)) {
                            draft.clear()
                            session.refreshKkms()
                            onDone()
                        }
                    }
                }
            )
        }
    }
}

/**
 * Выдаёт токен и заводит кассу в узле одним действием.
 *
 * Между выдачей токена и заведением кассы токен нигде не задерживается:
 * ни на экране, ни в настройках рабочего места.
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
        ofdId = registration.provider,
        ofdEnvironment = registration.environment,
        ofdSystemId = systemId,
        ofdToken = issued.token.toString(),
        adminPin = registration.adminPin
    )
    // В подпись сообщения идёт название шага, а не пин: строка отказа
    // видна на экране, и пин администратора в ней быть не должен.
    return session.guard(what) { session.client.initKkm(request, BOOTSTRAP_PIN) } != null
}

/** Оператор, чей кабинет заводит кассы в этом рабочем месте. */
private const val CABINET_PROVIDER = "BFD"

/** Чем и от чьего имени касса заводится в узле. */
private data class Registration(val provider: String, val environment: String, val adminPin: String)

/**
 * Пин, которым узел подтверждает право заводить кассы.
 *
 * Это не пин будущей кассы: её администратор получает тот, что набран
 * в поле рядом.
 */
private const val BOOTSTRAP_PIN = "0000"
