package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kz.mybrain.superkassa.designsystem.keyboard.escapePressedBy
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.NavKeys
import kz.mybrain.superkassa.navigation.Navigator
import kz.mybrain.superkassa.navigation.section.DashboardKey
import kz.mybrain.superkassa.navigation.step.StepKey
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.cabinetLook
import kz.mybrain.superkassa.presentation.cabinet.cabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.steps
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.look.lookViewModel
import kz.mybrain.superkassa.presentation.common.message.MessageEffect
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.navigation.LocalScreenBar
import kz.mybrain.superkassa.presentation.common.navigation.LocalToKassa
import kz.mybrain.superkassa.presentation.common.navigation.ScreenBarState
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.bar.WorkBar
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.closeStep
import kz.mybrain.superkassa.presentation.shell.section.currentSection
import kz.mybrain.superkassa.presentation.shell.section.openSection
import kz.mybrain.superkassa.presentation.shell.section.outside
import kz.mybrain.superkassa.presentation.shell.section.sectionEntries
import kz.mybrain.superkassa.presentation.shell.section.sectionsFor
import kz.mybrain.superkassa.presentation.shell.section.stepBack
import kz.mybrain.superkassa.presentation.update.check.updatesViewModel

/**
 * Каркас окна.
 *
 * Разделы — рельсом слева, как в настольном Material 3; над содержимым —
 * шапка приложения с кассой, её состоянием и действиями. Состояние кассы
 * висит всегда и на всех разделах: кассир должен увидеть блокировку
 * до того, как пробьёт чек, а не после.
 */
@Composable
fun ShellScreen(app: AppContainer) {
    // Кабинет живёт рядом с кассой, а не внутри неё: вход туда свой —
    // по ЭЦП владельца, — и переживает переходы между разделами.
    val shellModel = shellViewModel(app)
    val look = lookViewModel(app.services.look)
    // Кабинета нет на платформе, где его не собрали: там нет и его модели.
    val cabinet = app.areas.cabinet?.let { cabinetViewModel(app.services, it) }
    val window = remember(app, shellModel, look, cabinet) {
        WindowParts(shellModel, look, cabinet?.let { CabinetWindow(it, cabinetLook(look), cabinetNeighbours(app)) })
    }
    val shell by window.shell.state.collectAsScreenState()
    // Один хост сообщений на окно: снекбар лежит поверх содержимого
    // и не сдвигает разметку под руками кассира.
    val messages = remember { SnackbarHostState() }
    // Проверка выпусков живёт, пока открыто окно, и начинается с него —
    // ещё до входа: модель окна сама ждёт своего часа и молчит без сети.
    updatesViewModel(app.services, app.areas.update)
    if (shell.seat.signedIn) {
        WorkShell(app, window, shell, messages)
    } else {
        DoorShell(app, window, messages)
    }
}

/**
 * Модели окна, которые нужны каркасу целиком: сам каркас, вид окна и кабинет.
 *
 * Собраны вместе, потому что их зовут шапка, рельс и разделы одновременно:
 * по отдельности они протягивались бы тремя параметрами через каждый слой.
 *
 * @property cabinet кабинет окна; `null` — на этой платформе кабинета нет.
 */
internal class WindowParts(val shell: ShellViewModel, val look: LookViewModel, val cabinet: CabinetWindow?) {

    /** Шаги кабинета для мастера подключения: те же вход и формы, что в разделах кабинета. */
    val steps: CabinetSteps? = cabinet?.steps()
}

/**
 * Рабочее окно: навигация по разделам, одна шапка и раздел под ней.
 *
 * Рамка — [ShellFrame] на `NavigationSuiteScaffold` Material 3, переходы —
 * Navigation 3: история «назад» — список ключей, который держит окно,
 * `NavDisplay` рисует её вершину. Жест Android, Escape и стрелка в шапке
 * ведут по одной этой истории. Шапка называет открытый раздел, а под
 * ним — кассу и кассира: из шапки видно, где кассир и за какой кассой.
 *
 * Модели областей живут в хранилище окна, а не в записи истории: корзина
 * продажи переживает уход в журнал и возврат обратно.
 */
@Composable
private fun WorkShell(app: AppContainer, window: WindowParts, shell: ShellUiState, messages: SnackbarHostState) {
    val release by updatesViewModel(app.services, app.areas.update).state.collectAsScreenState()
    val sections = sectionsFor(shell.seat.isAdmin, app.areas)
    val history = rememberNavBackStack(NavKeys, DashboardKey)
    LaunchedEffect(sections) { if (history.outside(sections)) history.openSection(Section.Dashboard) }
    val section = history.currentSection()
    // Отказ относится к действию, а не к окну: уходя с экрана своей рукой,
    // кассир оставлял за собой отказ настроек, и тот висел поверх аналитики
    // до нажатия. Переход, сделанный самим приложением, сообщение не гасит.
    val back = { if (history.stepBack()) window.shell.sectionPicked() }
    val stepped = history.last() is StepKey
    StepsOf(history, back) {
        ShellFrame(
            sections = sections,
            current = section,
            onPick = { picked -> window.shell.sectionPicked().also { history.openSection(picked) } },
            marked = if (release.available != null) setOf(Section.Settings) else emptySet(),
            topBar = { onMenu -> WorkBar(app, window, shell, section, onMenu, back.takeIf { stepped }) },
            snackbarHost = { MessageHost(messages) }
        ) { padding ->
            ShellMessages(app, messages)
            SectionDisplay(app, window, history, Modifier.padding(padding), back)
        }
    }
}

/**
 * Шаги внутри разделов — по истории окна: экран открывает ключ шага
 * через [LocalNavigator], а называет себя в шапке через [LocalScreenBar].
 */
@Composable
internal fun StepsOf(history: MutableList<NavKey>, back: () -> Unit, content: @Composable () -> Unit) {
    val navigator = remember(history) {
        object : Navigator {
            override fun open(key: NavKey) {
                history.add(key)
            }

            override fun back() = back()

            override fun close(key: NavKey) = history.closeStep(key)
        }
    }
    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalScreenBar provides remember { ScreenBarState() },
        content = content
    )
}

/** Раздел под шапкой: вершина истории окна, нарисованная `NavDisplay`. */
@Composable
private fun SectionDisplay(
    app: AppContainer,
    window: WindowParts,
    history: MutableList<NavKey>,
    modifier: Modifier,
    back: () -> Unit
) {
    CompositionLocalProvider(LocalToKassa provides { history.openSection(Section.Dashboard) }) {
        WindowDisplay(history, back, modifier) { step -> sectionEntries(app, window, step) }
    }
}

/**
 * Escape — шаг назад по истории окна, как жест Android и стрелка в шапке.
 *
 * Нажатие берётся на всплытии: Escape раскрытого списка, диалога или
 * открытого поверх списка раздела настроек достаётся им, а не окну.
 */
internal fun Modifier.backOnEscape(enabled: Boolean, back: () -> Unit): Modifier = onKeyEvent { event ->
    (enabled && escapePressedBy(event)).also { if (it) back() }
}

/**
 * Показ сообщений окна.
 *
 * Помехи кабинета идут тем же путём, что и отказы кассы: модели кабинета
 * пишут их в ту же строку сообщений, и место ему одно — внизу окна.
 */
@Composable
internal fun ShellMessages(app: AppContainer, messages: SnackbarHostState) {
    val message by app.services.talk.notices.current.collectAsScreenState()
    MessageEffect(message, messages) { app.services.talk.notices.clear() }
}
