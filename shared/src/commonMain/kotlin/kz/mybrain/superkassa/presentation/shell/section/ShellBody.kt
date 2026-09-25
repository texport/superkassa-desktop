package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.windowMargin
import kz.mybrain.superkassa.navigation.step.PlaceCardKey
import kz.mybrain.superkassa.navigation.step.ReturnBasisKey
import kz.mybrain.superkassa.navigation.step.SettingsSectionKey
import kz.mybrain.superkassa.navigation.step.SetupStepKey
import kz.mybrain.superkassa.navigation.step.StepKey
import kz.mybrain.superkassa.presentation.cabinet.CabinetScreen
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
import kz.mybrain.superkassa.presentation.setup.ConnectKkm
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardScreen
import kz.mybrain.superkassa.presentation.shift.dashboard.dashboardViewModel
import kz.mybrain.superkassa.presentation.users.UsersScreen
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
    SectionPlace(app) { SectionScreen(app, window, section, step) }
}

/**
 * Место раздела в окне — и рабочего, и до входа.
 *
 * Печатная форма живёт над всеми разделами: кассир открывает её
 * из журнала и вправе уйти в продажу, не теряя окна. То же окно стоит
 * и над кабинетом до входа — оно одно на оба входа.
 */
@Composable
internal fun SectionPlace(app: AppContainer, content: @Composable () -> Unit) {
    PrintDesk(app.services, app.areas.print) {
        Box(modifier = Modifier.sectionFrame().fillMaxHeight()) { content() }
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
        Section.Register -> Connect(app, window, step = step as? SetupStepKey)
        Section.Cabinet -> window.cabinet?.let { CabinetScreen(it, step is PlaceCardKey) }
        Section.Settings -> SettingsScreen(settingsOf(app, window), step as? SettingsSectionKey)
    }
}

/**
 * Мастер подключения окна; на платформе без мастера не рисуется ничего: двери к нему там нет.
 *
 * @param step шаг мастера поверх первого; `null` — первый шаг.
 */
@Composable
internal fun Connect(app: AppContainer, window: WindowParts, onBack: (() -> Unit)? = null, step: SetupStepKey? = null) {
    val ports = app.areas.setup ?: return
    ConnectKkm(app.services, ports, window.steps, step = step?.step, onBack = onBack)
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
