package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSession
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.presentation.common.model.WindowServices
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationActions
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationUiState
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationViewModel
import kz.mybrain.superkassa.presentation.setup.registration.registrationViewModel

/**
 * Один шаг мастера окна: модель — из портов мастера, обращения к кабинету —
 * шагами кабинета окна.
 *
 * Каждый шаг — своя запись истории окна; модели у шагов общие, одни на окно.
 * Без кабинета мастер ведёт только ручной путь.
 *
 * @param cabinet шаги кабинета окна; `null` — на этой платформе кабинета нет.
 * @param onBack куда уйти, когда касса заведена; `null` — остаться в разделе.
 * @param step шаг поверх первого; `null` — первый шаг.
 */
@Composable
fun ConnectKkm(
    services: WindowServices,
    ports: SetupPorts,
    cabinet: CabinetSteps?,
    step: String? = null,
    onBack: (() -> Unit)? = null
) {
    if (cabinet == null || ports.cabinet == null) {
        return ConnectByHand(setupViewModel(services, ports, WithoutCabinet), step, onBack)
    }
    val calls = cabinet.calls
    val models = SetupModels(setupViewModel(services, ports, calls), registrationViewModel(ports, calls))
    ConnectKkmScreen(models, cabinet, step, onBack)
}

/** Мастер без кабинета: путь один — вручную, и выбора пути в нём нет. */
@Composable
internal fun ConnectByHand(model: SetupViewModel, step: String? = null, onBack: (() -> Unit)? = null) {
    val state by model.state.collectAsScreenState()
    // Контуры и кассы читаются, как шаг открыт: справочник мог не ответить прежде.
    LaunchedEffect(Unit) { model.reload() }
    SetupContent(SetupParts(state, model, office = null, onBack = onBack), step)
}

/**
 * Подключение кассы через кабинет: от заводского номера до входа кассира.
 *
 * Мастер переживает закрытие приложения: заявление в КГД рассматривают
 * не в ту же минуту, а ключ ЭЦП бывает у владельца, который придёт завтра.
 * Пройденное хранит модель, токен туда не попадает.
 *
 * Вход в кабинет, касса в кабинете и ожидание подписи — шаги кабинета окна
 * [cabinet]: его формы — те же, что в разделах кабинета, и своя копия каждой
 * разошлась бы с ними. Подпись идёт через порт подписи кабинета — тот же,
 * которым входят в кабинет и подают заявления из его разделов.
 */
@Composable
internal fun ConnectKkmScreen(
    models: SetupModels,
    cabinet: CabinetSteps,
    step: String? = null,
    onBack: (() -> Unit)? = null
) {
    val state by models.setup.state.collectAsScreenState()
    val registration by models.registration.state.collectAsScreenState()
    // Контуры и кассы читаются, как шаг открыт: справочник, не прочитанный
    // вчера, сегодня мог и ответить.
    LaunchedEffect(Unit) { models.setup.reload() }
    val office = SetupOffice(cabinet, cabinet.session(), registration, models.registration)
    SetupContent(SetupParts(state, models.setup, office, onBack), step)
}

/** Модели мастера: пройденное и заведение кассы — одна, заявление о постановке на учёт — другая. */
internal class SetupModels(val setup: SetupViewModel, val registration: RegistrationViewModel)

/**
 * Всё, из чего собран шаг мастера: состояние, действия и кабинет.
 *
 * @property office кабинет окна и заявление о постановке на учёт; `null` —
 *   кабинета нет, и путь один — вручную.
 * @property onBack куда уйти, когда касса заведена; `null` — остаться в разделе.
 */
class SetupParts(
    val state: SetupUiState,
    val actions: SetupActions,
    val office: SetupOffice?,
    val onBack: (() -> Unit)?
) {
    /** Касса в кабинете встала на учёт: кабинет выпустит ей токен. */
    val onRecord: Boolean get() = office?.registration?.onRecord(state.draft.cabinetRegisterId) == true
}

/**
 * Кабинет окна глазами шагов мастера.
 *
 * @property cabinet вход, форма кассы и ожидание подписи — шаги кабинета окна.
 * @property session вошёл ли владелец и занят ли кабинет.
 * @property registration касса в кабинете и заявление о постановке её на учёт.
 * @property actions подать заявление и перечитать кассу.
 */
class SetupOffice(
    val cabinet: CabinetSteps,
    val session: CabinetSession,
    val registration: RegistrationUiState,
    val actions: RegistrationActions
)
