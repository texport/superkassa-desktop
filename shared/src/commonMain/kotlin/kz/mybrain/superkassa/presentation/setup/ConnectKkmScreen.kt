package kz.mybrain.superkassa.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.CardSequence
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.picker.WideChoiceSegments
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.motion.Durations
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.signin.SignInAction
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.setup.component.AdminStepCard
import kz.mybrain.superkassa.presentation.setup.component.ApplicationStepCard
import kz.mybrain.superkassa.presentation.setup.component.CabinetStepCard
import kz.mybrain.superkassa.presentation.setup.component.FactoryStepCard
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationActions
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationUiState
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationViewModel
import kz.mybrain.superkassa.presentation.setup.registration.registrationViewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Мастер окна: модель — из портов мастера, обращения к кабинету — через кабинет окна.
 *
 * На платформе без мастера не рисуется ничего: двери к нему там нет.
 * Без кабинета мастер ведёт только ручной путь ([ConnectByHand]).
 *
 * @param cabinet кабинет окна; `null` — на этой платформе кабинета нет.
 * @param onBack возврат туда, откуда пришли; `null` — возвращаться некуда.
 */
@Composable
fun ConnectKkm(app: AppContainer, cabinet: CabinetWindow?, onBack: (() -> Unit)? = null) {
    val ports = app.areas.setup ?: return
    if (cabinet == null || ports.cabinet == null) {
        return ConnectByHand(setupViewModel(app, ports, WithoutCabinet), onBack)
    }
    val calls = cabinet.cabinet.calls()
    val models = SetupModels(setupViewModel(app, ports, calls), registrationViewModel(ports, calls))
    ConnectKkmScreen(models, cabinet, onBack)
}

/**
 * Подключение кассы: от заводского номера до входа кассира.
 *
 * Мастер переживает закрытие приложения: заявление в ИСНА рассматривают
 * не в ту же минуту, а ключ ЭЦП бывает у владельца, который придёт завтра.
 * Пройденное хранит модель, токен туда не попадает.
 *
 * Вход в кабинет, точку и кассу в кабинете ведёт кабинет окна [cabinet]:
 * его формы — те же, что в разделах кабинета, и своя копия каждой
 * разошлась бы с ними.
 *
 * @param onBack возврат туда, откуда пришли; `null` — возвращаться некуда.
 */
@Composable
fun ConnectKkmScreen(models: SetupModels, cabinet: CabinetWindow, onBack: (() -> Unit)? = null) {
    val state by models.setup.state.collectAsScreenState()
    val registration by models.registration.state.collectAsScreenState()
    // Контуры и кассы читаются, как мастер открыт: справочник, не прочитанный
    // вчера, сегодня мог и ответить.
    LaunchedEffect(Unit) { models.setup.reload() }
    val window by cabinet.cabinet.state.collectAsScreenState()
    SetupContent(SetupParts(state, models.setup, registration, models.registration, cabinet, window, onBack))
}

/** Модели мастера: пройденное и заведение кассы — одна, заявление о постановке на учёт — другая. */
class SetupModels(val setup: SetupViewModel, val registration: RegistrationViewModel)

/** Обращения мастера идут работой кабинета окна: его занятость и его слова о помехах. */
private fun CabinetViewModel.calls(): CabinetCalls = object : CabinetCalls {
    override suspend fun <T> run(action: String, block: suspend () -> T): T? = work.run(action, block).value
}

/** Всё, из чего собран мастер: состояние, действия и кабинет окна для шагов в кабинете. */
class SetupParts(
    val state: SetupUiState,
    val actions: SetupActions,
    val registration: RegistrationUiState,
    val registrationActions: RegistrationActions,
    val cabinet: CabinetWindow,
    val window: CabinetUiState,
    val onBack: (() -> Unit)?
)

/**
 * Мастер целиком: шапка раздела, выбор пути и шаги.
 *
 * Шапка — такая же строка названия, как у любого раздела, а не вторая
 * полоса приложения: мастер был единственным разделом со своей строкой
 * заголовка. Стоит она над прокруткой: мастер длинный, и уехавшая вверх
 * стрелка возврата означала бы, что выйти можно, только домотав до конца.
 *
 * Мастер во всю ширину раздела: на широком окне шаги встают рядом,
 * и половина экрана не пустует.
 */
@Composable
fun SetupContent(parts: SetupParts) {
    val setup = textsOf(LocalLanguage.current).setup
    val state = parts.state
    SetupFrame(state, parts.actions, parts.onBack) {
        WideChoiceSegments(
            options = SetupWay.entries,
            selected = state.way,
            label = { it.title(setup) },
            onSelect = parts.actions::chooseWay
        )
        when (state.way) {
            SetupWay.ViaCabinet -> ViaCabinet(parts)
            SetupWay.ByHand -> ByHand(parts.state, parts.actions)
        }
    }
}

/** Рамка мастера: шапка раздела над прокруткой шагов и вопрос «начать заново». */
@Composable
internal fun SetupFrame(
    state: SetupUiState,
    actions: SetupActions,
    onBack: (() -> Unit)?,
    steps: @Composable ColumnScope.() -> Unit
) {
    val setup = textsOf(LocalLanguage.current).setup
    if (state.startingOver) StartOverDialog(actions)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        SetupHeading(setup, state.draft.factoryNumber != null, actions, onBack)
        ScrollableColumn(modifier = Modifier.weight(1f), spacing = Spacing.fieldGap, content = steps)
    }
}

/**
 * Путь через кабинет: четыре шага подряд.
 *
 * Вход нужен всем шагам, кроме первого, и живёт он здесь: спрятанный внутри
 * пройденного шага, он исчезал вместе с ним — мастер, продолженный назавтра,
 * упирался в «ждёт предыдущего шага» без единой кнопки. Но только там, где
 * своей кнопки нет: пока касса в кабинете не заведена, вход предлагает сам
 * шаг — под объяснением, зачем он.
 */
@Composable
private fun ViaCabinet(parts: SetupParts) {
    val setup = textsOf(LocalLanguage.current).setup
    val draft = parts.state.draft
    val open = parts.window.open
    if (!open && draft.cabinetRegisterId != null) {
        // Та же кнопка, что на двери кабинета: со сроком ожидания и отменой.
        val language = LocalLanguage.current
        SignInAction(parts.cabinet.cabinet, language, textsOf(language).cabinet, modifier = Modifier)
    }
    // Касса в кабинете перечитывается, пока номера КГД нет: оба последних
    // шага ждут его, и владелец не должен открывать мастер заново.
    val registerId = draft.cabinetRegisterId
    LaunchedEffect(registerId, open) {
        if (registerId == null || !open) return@LaunchedEffect
        parts.registrationActions.readRecord(registerId)
        parts.registrationActions.watchRecord(registerId, Durations.whileWatching)
    }
    val busy = parts.window.busy
    val onRecord = parts.registration.onRecord(registerId)
    CardSequence(Modifier.fillMaxWidth()) {
        FactoryStepCard(parts.state, parts.actions, setup)
        CabinetStepCard(parts.cabinet, setup, draft, parts.actions::rememberRegister)
        ApplicationStepCard(registerId, parts.registration, parts.registrationActions, setup, open, busy)
        AdminStepCard(parts.state, parts.actions, setup, onRecord, busy) { parts.onBack?.invoke() }
    }
}
