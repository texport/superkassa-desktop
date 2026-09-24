package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.windowMargin
import kz.mybrain.superkassa.designsystem.keyboard.SystemBack
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.cabinetViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.settings.look.LookViewModel
import kz.mybrain.superkassa.presentation.settings.look.lookViewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.bar.BusyLine
import kz.mybrain.superkassa.presentation.shell.bar.WorkBar
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.LocalSectionSwitch
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.SectionContent
import kz.mybrain.superkassa.presentation.shell.section.SectionDoor
import kz.mybrain.superkassa.presentation.shell.section.SectionTrail
import kz.mybrain.superkassa.presentation.shell.section.SectionTrailSaver
import kz.mybrain.superkassa.presentation.shell.section.sectionsFor
import kz.mybrain.superkassa.presentation.update.check.RailVersion
import kz.mybrain.superkassa.presentation.update.check.UpdateDialog
import kz.mybrain.superkassa.presentation.update.check.UpdatesUiState
import kz.mybrain.superkassa.presentation.update.check.UpdatesViewModel
import kz.mybrain.superkassa.presentation.update.check.updatesViewModel
import kz.mybrain.superkassa.presentation.users.signin.loginViewModel
import kz.mybrain.superkassa.strings.api.textsOf

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
    val look = lookViewModel(app)
    // Кабинета нет на платформе, где его не собрали: там нет и его модели.
    val cabinet = app.areas.cabinet?.let { cabinetViewModel(app) }
    val window = remember(app, shellModel, look, cabinet) {
        WindowParts(shellModel, look, cabinet?.let { CabinetWindow(app, it, cabinetLook(look)) })
    }
    val shell by window.shell.state.collectAsScreenState()
    // Один хост сообщений на окно: снекбар лежит поверх содержимого
    // и не сдвигает разметку под руками кассира.
    val messages = remember { SnackbarHostState() }
    // Проверка выпусков живёт, пока открыто окно, и начинается с него —
    // ещё до входа: модель окна сама ждёт своего часа и молчит без сети.
    updatesViewModel(app)
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
class WindowParts(val shell: ShellViewModel, val look: LookViewModel, val cabinet: CabinetWindow?)

/**
 * Окно до входа: кассир видит только вход.
 *
 * Пустые разделы без выбранной кассы отвечают отказами и ничему не учат,
 * поэтому ни рельса, ни шапки кассы здесь нет.
 */
@Composable
internal fun DoorShell(app: AppContainer, window: WindowParts, messages: SnackbarHostState) {
    // Набранное кассиром живёт в модели входа окна: список касс и полоса
    // пина читают одно и то же. Поле окна — то же, что у разделов, и сверху:
    // шапки над входом нет.
    val login = loginViewModel(app)
    val door by login.state.collectAsScreenState()
    Scaffold(
        topBar = { BusyLine(door.entering) },
        snackbarHost = { MessageHost(messages) }
    ) { padding ->
        ShellMessages(app, messages)
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(windowMargin)) {
            SectionDoor(app, window, door, login)
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
private fun WorkShell(app: AppContainer, window: WindowParts, shell: ShellUiState, messages: SnackbarHostState) {
    val updates = updatesViewModel(app)
    val release by updates.state.collectAsScreenState()
    val look by window.look.state.collectAsScreenState()
    var updateShown by rememberSaveable { mutableStateOf(false) }
    val sections = sectionsFor(shell.seat.isAdmin, app.areas)
    // Отказ относится к действию, а не к окну: уходя с экрана своей рукой,
    // кассир оставлял за собой отказ настроек, и тот висел поверх аналитики
    // до нажатия. Переход, сделанный самим приложением, сообщение не гасит:
    // там оно как раз об итоге действия.
    var trail by rememberSectionTrail(sections, window.shell::sectionPicked)
    val section = trail.current
    val pick = { picked: Section ->
        window.shell.sectionPicked()
        trail = trail.open(picked)
    }
    Scaffold(topBar = { WorkBar(app, window, shell, section) }, snackbarHost = { MessageHost(messages) }) {
        ShellMessages(app, messages)
        Row(modifier = Modifier.fillMaxSize().padding(it)) {
            val footer: @Composable ColumnScope.() -> Unit = { RailVersion(release) { updateShown = true } }
            SectionRail(sections, section, look.railCollapsed, window.look::toggleRail, footer, pick)
            CompositionLocalProvider(LocalSectionSwitch provides { asked -> trail = trail.open(asked) }) {
                SectionContent(app, window, section)
            }
        }
        UpdateOffer(release, updates, updateShown) { updateShown = false }
    }
}

/**
 * Путь по разделам окна.
 *
 * Хранится так, чтобы пережить поворот экрана: на Android поворот
 * пересоздаёт активность, и кассир оказывался на главном экране посреди
 * продажи. Жест «назад» ведёт по нему к предыдущему разделу — своей рукой,
 * поэтому итог прежнего действия снимается ([onLeave]). Разделы, которых
 * вошедшему больше не видно, из пути уходят.
 */
@Composable
private fun rememberSectionTrail(sections: List<Section>, onLeave: () -> Unit): MutableState<SectionTrail> {
    val trail = rememberSaveable(stateSaver = SectionTrailSaver) { mutableStateOf(SectionTrail()) }
    if (trail.value.sections.any { it !in sections }) trail.value = trail.value.within(sections)
    SystemBack(enabled = trail.value.canGoBack) {
        onLeave()
        trail.value = trail.value.back()
    }
    return trail
}

/** Предложение обновиться — когда кассир сам спросил о выпуске. */
@Composable
private fun UpdateOffer(release: UpdatesUiState, updates: UpdatesViewModel, shown: Boolean, onClose: () -> Unit) {
    val update = release.available
    if (shown && update != null) UpdateDialog(update, textsOf(LocalLanguage.current).update, updates::install, onClose)
}

/**
 * Показ сообщений окна.
 *
 * Помехи кабинета идут тем же путём, что и отказы кассы: модели кабинета
 * пишут их в ту же строку сообщений, и место ему одно — внизу окна.
 */
@Composable
private fun ShellMessages(app: AppContainer, messages: SnackbarHostState) {
    val message by app.notices.current.collectAsScreenState()
    MessageEffect(message, messages) { app.notices.clear() }
}
