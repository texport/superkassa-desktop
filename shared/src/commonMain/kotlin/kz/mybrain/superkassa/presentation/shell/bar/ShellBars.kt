package kz.mybrain.superkassa.presentation.shell.bar

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.section.BarLead
import kz.mybrain.superkassa.designsystem.state.BusyLine
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.domain.kkm.model.orgTitle
import kz.mybrain.superkassa.presentation.cabinet.CabinetBar
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.navigation.LocalScreenBar
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.ShellUiState
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.users.signin.loginViewModel

/**
 * Шапка окна: одна на всё приложение.
 *
 * Она называет то, чем владелец сейчас распоряжается, и она же держит
 * возврат: в кабинете это компания и он сам, в остальных разделах —
 * касса и кассир. Экраны под шапкой своих стрелок не рисуют.
 */
@Composable
internal fun ShellBar(
    window: WindowParts,
    shell: ShellUiState,
    section: Section,
    onSignOut: () -> Unit,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val door = window.cabinet
    val office = door?.cabinet?.state?.collectAsScreenState()?.value
    val texts = LocalStrings.current
    Column {
        if (door != null && section == Section.Cabinet && office?.open == true) {
            CabinetBar(door.cabinet, door.look, onMenu = onMenu)
        } else {
            val step = LocalScreenBar.current
            val heading = BarHeading(step.title ?: section.title(texts.sections), step.subtitle)
            KkmTopBar(shell, window.look, onSignOut, window.shell::refresh, heading, barLead(onMenu, onBack))
        }
        // Кабинет живёт своей моделью, а полоска у окна одна: владелец
        // ждёт ответа кабинета так же, как кассир — ответа кассы.
        BusyLine(shell.busy || office?.busy == true)
    }
}

/**
 * Начало шапки: стрелка назад, когда открыт шаг внутри раздела, иначе —
 * кнопка разделов на телефоне. Как в настройках Pixel: из шага возвращаются
 * стрелкой, разделы открывают только с их верхнего уровня.
 */
@Composable
private fun barLead(onMenu: (() -> Unit)?, onBack: (() -> Unit)?): BarLead? {
    val texts = LocalStrings.current
    return when {
        onBack != null -> BarLead.Back(onBack, texts.settingsScreen.back)
        onMenu != null -> BarLead.Menu(onMenu, texts.sections.menu)
        else -> null
    }
}

/**
 * Шапка рабочего окна: кассир уходит сценарием входа — вход и уход одна область, одна модель.
 *
 * @param onBack шаг назад по истории окна; `null` — открыт раздел, а не шаг в нём.
 */
@Composable
internal fun WorkBar(
    app: AppContainer,
    window: WindowParts,
    shell: ShellUiState,
    section: Section,
    onMenu: (() -> Unit)?,
    onBack: (() -> Unit)?
) = ShellBar(window, shell, section, loginViewModel(app.services)::signOut, onMenu, onBack)

/**
 * Что называет шапка: открытый раздел или шаг и, при нужде, чьё это.
 *
 * @property subtitle чьё открытое — например, «Настройки кассы «Касса
 *   у входа»»; `null` — под заголовком касса и кассир.
 */
internal class BarHeading(val title: String, val subtitle: String? = null)

/**
 * Шапка приложения: где кассир, за какой кассой и в каком она состоянии.
 *
 * Заголовок — открытый раздел, подзаголовок — касса и кассир: из шапки
 * видно, что это за экран и чьи в нём данные. Шаг внутри раздела говорит
 * под заголовком сам, чьё он, — тогда кассир не нужен. Без раздела — на
 * экране, нарисованном отдельно от окна, — заголовок называет кассу, а под
 * ним организацию. Плашки состояния стоят до действий: кассир читает слева
 * направо и должен узнать о блокировке раньше, чем дотянется до кнопки.
 * Имя кассира не сокращается: длинное название уступает ему место;
 * на телефоне его в шапке нет вовсе.
 *
 * @param heading открытый раздел или шаг; `null` — заголовок называет кассу.
 * @param lead стрелка назад или кнопка разделов.
 */
@Composable
internal fun KkmTopBar(
    shell: ShellUiState,
    look: LookViewModel,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit,
    heading: BarHeading? = null,
    lead: BarLead? = null
) {
    val kkm = shell.kkmName ?: LocalStrings.current.topBar.noKkm
    val own = heading?.subtitle
    // На телефоне имя кассира не держит строку: не сокращаясь, оно
    // вытесняло название кассы целиком, а кто за кассой, кассир знает.
    val roomy = LocalWindowClass.current.width > WidthClass.Compact
    AppTopBar(
        title = heading?.title ?: kkm,
        subtitle = own ?: if (heading == null) shell.kkm?.orgTitle else kkm,
        subtitleKept = shell.cashier.takeIf { own == null && roomy },
        lead = lead
    ) {
        KkmBarActions(shell, look, onSignOut, onRefresh)
    }
}
