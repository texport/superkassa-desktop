package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.designsystem.adaptive.windowMargin
import kz.mybrain.superkassa.designsystem.keyboard.SystemBack
import kz.mybrain.superkassa.navigation.step.PlaceCardKey
import kz.mybrain.superkassa.navigation.step.ReturnBasisKey
import kz.mybrain.superkassa.navigation.step.SettingsSectionKey
import kz.mybrain.superkassa.navigation.step.StepKey
import kz.mybrain.superkassa.presentation.cabinet.CabinetScreen
import kz.mybrain.superkassa.presentation.cabinet.signin.CabinetDoor
import kz.mybrain.superkassa.presentation.common.print.LocalPrint
import kz.mybrain.superkassa.presentation.journal.HistoryScreen
import kz.mybrain.superkassa.presentation.journal.documents.journalViewModel
import kz.mybrain.superkassa.presentation.journal.queue.QueueScreen
import kz.mybrain.superkassa.presentation.journal.queue.queueViewModel
import kz.mybrain.superkassa.presentation.journal.shifts.shiftsViewModel
import kz.mybrain.superkassa.presentation.kassa.cash.CashScreen
import kz.mybrain.superkassa.presentation.kassa.cash.cashViewModel
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsScreen
import kz.mybrain.superkassa.presentation.kassa.refund.returnsViewModel
import kz.mybrain.superkassa.presentation.kassa.sale.ReceiptOutput
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScreen
import kz.mybrain.superkassa.presentation.kassa.sale.saleViewModel
import kz.mybrain.superkassa.presentation.print.preview.PrintDesk
import kz.mybrain.superkassa.presentation.settings.SettingsScreen
import kz.mybrain.superkassa.presentation.settings.WorkplaceSettingsScreen
import kz.mybrain.superkassa.presentation.setup.ConnectKkm
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardScreen
import kz.mybrain.superkassa.presentation.shift.dashboard.dashboardViewModel
import kz.mybrain.superkassa.presentation.users.UsersScreen
import kz.mybrain.superkassa.presentation.users.signin.Door
import kz.mybrain.superkassa.presentation.users.signin.LoginActions
import kz.mybrain.superkassa.presentation.users.signin.LoginScreen
import kz.mybrain.superkassa.presentation.users.signin.LoginUiState
import kz.mybrain.superkassa.presentation.users.usersViewModel

/**
 * Содержимое выбранного раздела.
 *
 * Своего отступа у каркаса нет: поля разделов уже задают его одним
 * значением, и второй отступ поверх делал колонку содержимого шире
 * положенного.
 *
 * @param step шаг внутри раздела — подробности поверх списка на узком окне.
 */
@Composable
internal fun SectionContent(
    app: AppContainer,
    window: WindowParts,
    section: Section,
    step: StepKey? = null
) {
    // Печатная форма живёт над всеми разделами: кассир открывает её
    // из журнала и вправе уйти в продажу, не теряя окна. То же окно
    // стоит и над дверью в кабинет — оно одно на оба входа.
    PrintDesk(app.services, app.areas.print) {
        Box(modifier = Modifier.sectionFrame().fillMaxHeight()) {
            SectionScreen(app, window, section, step)
        }
    }
}

/** Экран выбранного раздела. */
@Composable
private fun SectionScreen(app: AppContainer, window: WindowParts, section: Section, step: StepKey?) {
    when (section) {
        Section.Dashboard -> LocalPrint.current.let { print ->
            DashboardScreen(dashboardViewModel(app.services), print::preview) { print.print(it.id) }
        }
        Section.Sale -> LocalPrint.current.let { print ->
            val output = ReceiptOutput(show = { print.preview(it) }, print = print::print)
            SaleScreen(saleViewModel(app.services, app.areas.kassa), output)
        }
        Section.Returns -> ReturnsScreen(returnsViewModel(app.services, app.areas.kassa), step is ReturnBasisKey)
        Section.Cash -> CashScreen(cashViewModel(app.services))
        Section.History -> HistoryScreen(
            journalViewModel(app.services, app.areas.journal),
            shiftsViewModel(app.services),
            LocalPrint.current
        )
        Section.Queue -> QueueScreen(queueViewModel(app.services))
        Section.Users -> UsersScreen(usersViewModel(app.services))
        Section.Register -> Connect(app, window)
        Section.Cabinet -> window.cabinet?.let { CabinetScreen(it, step is PlaceCardKey) }
        Section.Settings -> SettingsScreen(settingsOf(app, window), step as? SettingsSectionKey)
    }
}

/**
 * Вход кассира занимает окно целиком: пустые разделы без кассы ничему не учат.
 *
 * @param close шаг назад — то же, что стрелка в шапке окна: снять шаг
 *   за дверью или закрыть её и перечитать кассы — за дверью кассу могли
 *   завести.
 * @param step шаг за дверью: раздел настроек поверх их списка.
 */
@Composable
internal fun SectionDoor(
    app: AppContainer,
    window: WindowParts,
    state: LoginUiState,
    actions: LoginActions,
    close: () -> Unit,
    step: NavKey? = null
) {
    // Форму владелец может открыть и отсюда — через дверь в кабинет.
    PrintDesk(app.services, app.areas.print) {
        LoginScreen(
            state = state,
            actions = actions,
            doors = doorsOf(app.areas),
            door = { door, _ -> LoginDoor(app, window, door, close, step) }
        )
    }
}

/** Закрыть дверь: вернуться к списку касс и перечитать его — за дверью кассу могли завести. */
internal fun closeDoor(login: LoginActions) {
    login.reload()
    login.open(Door.Kkms)
}

/**
 * Что открыто за дверью входа.
 *
 * Заведение кассы, кабинет и настройки рабочего места — чужие входу
 * разделы; вход только открывает дверь, а что за ней, знает каркас.
 */
@Composable
private fun LoginDoor(app: AppContainer, window: WindowParts, door: Door, close: () -> Unit, step: NavKey?) {
    // «Назад» за дверью возвращает к списку касс — туда же, куда стрелка.
    SystemBack(enabled = true, onBack = close)
    when (door) {
        Door.Register -> Connect(app, window, close)
        Door.Cabinet -> window.cabinet?.let { CabinetDoor(it, close, step is PlaceCardKey) }
        Door.Settings -> WorkplaceSettingsScreen(settingsOf(app, window), step as? SettingsSectionKey)
        Door.Kkms -> Unit
    }
}

/**
 * Двери входа, за которыми на этой платформе что-то есть.
 *
 * Кабинет входит подписью ЭЦП, а мастер подключения собран не везде:
 * дверь без портов за ней открывала бы пустое место.
 */
private fun doorsOf(areas: AreaPorts): Set<Door> = buildSet {
    if (areas.setup != null) add(Door.Register)
    if (areas.cabinet != null) add(Door.Cabinet)
    add(Door.Settings)
}

/** Мастер подключения окна; на платформе без мастера не рисуется ничего: двери к нему там нет. */
@Composable
private fun Connect(app: AppContainer, window: WindowParts, onBack: (() -> Unit)? = null) {
    val ports = app.areas.setup ?: return
    ConnectKkm(app.services, ports, window.steps, onBack)
}

/**
 * Место раздела правее рельса: вся ширина с полем окна.
 *
 * Поле окна — слева и справа, как у Material 3: сверху раздел отделяет
 * шапка. Ставит его каркас один раз, а не каждый экран сам: прежде слева
 * стояло 12, а справа то 12, то 28. Разделы идут на всю ширину — рабочие
 * столы прежде вставали посередине в пределах 1440 точек, и на мониторе
 * 1920 по бокам оставалось по 180 пустых точек, на 2560 — по 500.
 * Широкое окно делят панели самих экранов, а не пустые поля.
 */
@Composable
internal fun Modifier.sectionFrame(): Modifier = fillMaxWidth().padding(horizontal = windowMargin)
