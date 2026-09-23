package kz.mybrain.superkassa.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.data.cabinet.CabinetClient
import kz.mybrain.superkassa.presentation.cabinet.CabinetDocuments
import kz.mybrain.superkassa.presentation.login.SignInSlot
import kz.mybrain.superkassa.presentation.login.actions
import kz.mybrain.superkassa.presentation.login.loginViewModel
import kz.mybrain.superkassa.presentation.session.CabinetSession
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.session.adoptCabinetNames
import kz.mybrain.superkassa.presentation.session.loadDictionaries
import kz.mybrain.superkassa.presentation.session.railCollapsed
import kz.mybrain.superkassa.presentation.session.refreshKkms
import kz.mybrain.superkassa.presentation.session.refreshSelected
import kz.mybrain.superkassa.presentation.session.toggleRail
import kz.mybrain.superkassa.presentation.strings.updateTexts

/**
 * Каркас окна.
 *
 * Разделы — рельсом слева, как в настольном Material 3; над содержимым —
 * шапка приложения с кассой, её состоянием и действиями. Состояние узла
 * и кассы висит всегда и на всех разделах: кассир должен увидеть разрыв
 * связи до того, как пробьёт чек, а не после.
 */
@Composable
fun Shell(session: Session, app: AppContainer) {
    val cabinet = rememberCabinet(session)
    // Один хост сообщений на окно: снекбар лежит поверх содержимого
    // и не сдвигает разметку под руками кассира.
    val messages = remember { SnackbarHostState() }
    // Куда владелец углубился внутри кабинета — знает окно, а не экран
    // под ним: стрелка возврата в приложении одна и живёт в шапке.
    val documents = remember { CabinetDocuments() }
    ShellEffects(session)
    if (session.signedIn) {
        WorkShell(session, app, cabinet, documents, messages)
    } else {
        DoorShell(session, app, cabinet, messages)
    }
}

/**
 * Кабинет живёт рядом с кассовым сеансом, а не внутри него: вход туда
 * свой — по ЭЦП владельца, — и переживает переходы между разделами.
 */
@Composable
private fun rememberCabinet(session: Session): CabinetSession = remember {
    CabinetSession(CabinetClient(session.preferences.cabinetUrl)).apply {
        // Названия касс владелец даёт в кабинете, а нужны они кассиру
        // на входе, когда кабинет закрыт: прочитанное уходит узлу
        // и оттуда его видит любое рабочее место.
        onRegisterNames = { registers -> session.adoptCabinetNames(registers) }
    }
}

/** Что окно делает само, пока открыто: читает, следит, перечитывает. */
@Composable
private fun ShellEffects(session: Session) {
    LaunchedEffect(Unit) {
        session.refreshKkms()
        session.loadDictionaries()
    }
    // Проверка выпусков живёт, пока открыто окно: сама ждёт своего часа
    // и сама молчит, когда сети нет.
    LaunchedEffect(Unit) { session.updates.watch() }
    // Вход делается в кассе процесса; разделы, ещё живущие на узле,
    // читают его из сеанса, и сеанс повторяет каждый шаг держателя входа.
    LaunchedEffect(Unit) { session.followSignIn() }
    // Вошли — сразу подтягиваем состояние выбранной кассы для разделов
    // на узле. Главный экран читает кассу процесса сам.
    LaunchedEffect(session.selected?.kkmId, session.pin) {
        if (session.signedIn) session.refreshSelected()
    }
}

/**
 * Окно до входа: кассир видит только вход.
 *
 * Пустые разделы без выбранной кассы отвечают отказами и ничему не учат,
 * поэтому ни рельса, ни шапки кассы здесь нет.
 */
@Composable
internal fun DoorShell(session: Session, app: AppContainer, cabinet: CabinetSession, messages: SnackbarHostState) {
    // Набранное кассиром живёт в модели входа окна: полоса пина стоит
    // нижним слотом каркаса, а список касс — его содержимым, и оба
    // читают одно и то же.
    val login = loginViewModel(app)
    val door by login.state.collectAsState()
    val actions = remember(login) { login.actions() }
    Scaffold(
        topBar = { BusyLine(session.busy || door.entering) },
        // Полоса пина — слот каркаса, а не последний блок экрана: Material 3
        // кладёт снекбар над нижней полосой, и отказ входа перестал закрывать
        // ровно то, что кассир должен исправить, — поле пина и «Войти».
        bottomBar = { SignInSlot(door, actions) },
        snackbarHost = { MessageHost(messages) }
    ) { padding ->
        ShellMessages(session, cabinet, messages)
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionDoor(session, cabinet, door, actions)
        }
    }
}

/**
 * Рабочее окно: рельс разделов, шапка и содержимое.
 *
 * Шапка идёт во всю ширину окна, а рельс разделов — под ней: иначе шапка
 * начиналась правее рельса и накрывала его край, а окно выглядело
 * собранным из двух несогласованных половин.
 */
@Composable
private fun WorkShell(
    session: Session,
    app: AppContainer,
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    messages: SnackbarHostState
) {
    var section by remember { mutableStateOf(Section.Dashboard) }
    var updateShown by remember { mutableStateOf(false) }
    // Кассиру видны только его разделы: очередь, кассиров и настройки
    // касса отдаёт администратору, и пустой отказ вместо экрана ничему не учит.
    val sections = Section.entries.filter { session.isAdmin || !it.adminOnly }
    if (section !in sections) section = Section.Dashboard
    // Отказ относится к действию, а не к окну: уходя с экрана своей рукой,
    // кассир оставлял за собой отказ настроек, и тот висел поверх аналитики
    // до нажатия. Переход, сделанный самим приложением, сообщение не гасит:
    // там оно как раз об итоге действия.
    val pick = { picked: Section ->
        session.lastMessage = null
        cabinet.clearProblem()
        section = picked
    }
    Scaffold(topBar = { ShellBar(session, cabinet, documents, section) }, snackbarHost = { MessageHost(messages) }) {
        ShellMessages(session, cabinet, messages)
        Row(modifier = Modifier.fillMaxSize().padding(it)) {
            val footer: @Composable ColumnScope.() -> Unit = { RailVersion(session) { updateShown = true } }
            SectionRail(sections, section, session.railCollapsed, { session.toggleRail() }, footer, pick)
            CompositionLocalProvider(LocalSectionSwitch provides { asked -> section = asked }) {
                SectionContent(session, app, cabinet, documents, section)
            }
        }
        UpdateOffer(session, updateShown) { updateShown = false }
    }
}

/** Предложение обновиться — когда кассир сам спросил о выпуске. */
@Composable
private fun UpdateOffer(session: Session, shown: Boolean, onClose: () -> Unit) {
    val update = session.updates.available
    if (shown && update != null) UpdateDialog(update, updateTexts(session.language), onClose)
}

/**
 * Показ сообщений окна.
 *
 * Помехи кабинета идут тем же путём, что и отказы кассы: сообщение
 * в приложении одно, и место ему внизу окна.
 */
@Composable
private fun ShellMessages(session: Session, cabinet: CabinetSession, messages: SnackbarHostState) {
    val message by session.notices.current.collectAsState()
    MessageEffect(message, messages) { session.notices.clear() }
    CabinetMessageEffect(session, cabinet)
}
