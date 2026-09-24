package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.cabinet.CabinetScreen
import kz.mybrain.superkassa.presentation.cabinet.signin.CabinetDoor
import kz.mybrain.superkassa.presentation.common.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.common.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.common.keyboard.SystemBack
import kz.mybrain.superkassa.presentation.common.picker.LanguagePicker
import kz.mybrain.superkassa.presentation.common.picker.ThemeSwitch
import kz.mybrain.superkassa.presentation.debug.log.DebugSetting
import kz.mybrain.superkassa.presentation.journal.HistoryScreen
import kz.mybrain.superkassa.presentation.journal.documents.journalViewModel
import kz.mybrain.superkassa.presentation.journal.queue.QueueScreen
import kz.mybrain.superkassa.presentation.journal.queue.queueViewModel
import kz.mybrain.superkassa.presentation.journal.shifts.shiftsViewModel
import kz.mybrain.superkassa.presentation.kassa.cash.CashScreen
import kz.mybrain.superkassa.presentation.kassa.cash.cashViewModel
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsScreen
import kz.mybrain.superkassa.presentation.kassa.refund.returnsViewModel
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScreen
import kz.mybrain.superkassa.presentation.kassa.sale.saleViewModel
import kz.mybrain.superkassa.presentation.login.Door
import kz.mybrain.superkassa.presentation.login.LoginActions
import kz.mybrain.superkassa.presentation.login.LoginScreen
import kz.mybrain.superkassa.presentation.login.LoginUiState
import kz.mybrain.superkassa.presentation.print.preview.LocalPrint
import kz.mybrain.superkassa.presentation.print.preview.PrintDesk
import kz.mybrain.superkassa.presentation.print.target.PrintTargetSetting
import kz.mybrain.superkassa.presentation.settings.SettingsParts
import kz.mybrain.superkassa.presentation.settings.SettingsScreen
import kz.mybrain.superkassa.presentation.settings.WorkplaceSettingsScreen
import kz.mybrain.superkassa.presentation.settings.settingsBoard
import kz.mybrain.superkassa.presentation.settings.workplace.PanelBehaviourCard
import kz.mybrain.superkassa.presentation.setup.ConnectKkm
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardScreen
import kz.mybrain.superkassa.presentation.shift.dashboard.dashboardViewModel
import kz.mybrain.superkassa.presentation.update.check.UpdatesSetting
import kz.mybrain.superkassa.presentation.users.UsersScreen
import kz.mybrain.superkassa.presentation.users.usersViewModel

/**
 * Содержимое выбранного раздела.
 *
 * Своего отступа у каркаса нет: поля разделов уже задают его одним
 * значением, и второй отступ поверх делал колонку содержимого шире
 * положенного.
 */
@Composable
internal fun SectionContent(app: AppContainer, window: WindowParts, section: Section) {
    // Печатная форма живёт над всеми разделами: кассир открывает её
    // из журнала и вправе уйти в продажу, не теряя окна. То же окно
    // стоит и над дверью в кабинет — оно одно на оба входа.
    PrintDesk(app) {
        Box(modifier = Modifier.sectionWidth(section).fillMaxHeight()) {
            SectionScreen(app, window, section)
        }
    }
}

/** Экран выбранного раздела. */
@Composable
private fun SectionScreen(app: AppContainer, window: WindowParts, section: Section) {
    when (section) {
        Section.Dashboard -> LocalPrint.current.let { print ->
            DashboardScreen(dashboardViewModel(app), print::preview) { print.print(it.id) }
        }
        Section.Sale -> SaleScreen(saleViewModel(app))
        Section.Returns -> ReturnsScreen(returnsViewModel(app))
        Section.Cash -> CashScreen(cashViewModel(app))
        Section.History -> HistoryScreen(journalViewModel(app), shiftsViewModel(app), LocalPrint.current)
        Section.Queue -> QueueScreen(queueViewModel(app))
        Section.Users -> UsersScreen(usersViewModel(app))
        Section.Register -> ConnectKkm(app, window.cabinet)
        Section.Cabinet -> window.cabinet?.let { CabinetScreen(it) }
        Section.Settings -> SettingsScreen(settingsBoard(app, window.look, settingsParts(app)))
    }
}

/** Вход кассира занимает окно целиком: пустые разделы без кассы ничему не учат. */
@Composable
internal fun SectionDoor(app: AppContainer, window: WindowParts, state: LoginUiState, actions: LoginActions) {
    // Форму владелец может открыть и отсюда — через дверь в кабинет.
    PrintDesk(app) {
        LoginScreen(
            state = state,
            actions = actions,
            header = { LoginExtras(window) },
            doors = doorsOf(app.areas),
            // Закрытая дверь перечитывает кассы: за ней кассу могли завести.
            door = { door, close ->
                LoginDoor(app, window, door) {
                    actions.reload()
                    close()
                }
            }
        )
    }
}

/**
 * Что открыто за дверью входа.
 *
 * Заведение кассы, кабинет и настройки рабочего места — чужие входу
 * разделы; вход только открывает дверь, а что за ней, знает каркас.
 */
@Composable
private fun LoginDoor(app: AppContainer, window: WindowParts, door: Door, close: () -> Unit) {
    // «Назад» за дверью возвращает к списку касс — туда же, куда стрелка.
    SystemBack(enabled = true, onBack = close)
    when (door) {
        Door.Register -> ConnectKkm(app, window.cabinet, close)
        Door.Cabinet -> window.cabinet?.let { CabinetDoor(it, close) }
        Door.Settings -> WorkplaceSettingsScreen(settingsBoard(app, window.look, settingsParts(app)), close)
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

/** Рядом с названием входа: вид и язык — их меняют и до входа. */
@Composable
private fun RowScope.LoginExtras(window: WindowParts) {
    ThemeSwitch(window.look)
    LanguagePicker(window.look)
}

/**
 * Карточки других областей среди настроек: колонка продажи, принтер кассы, выпуски, журнал.
 *
 * Каждая со своей моделью окна; настройки о печати, обновлениях и отладке
 * не знают и только ставят карточку на её место.
 */
private fun settingsParts(app: AppContainer) = SettingsParts(
    panels = { PanelBehaviourCard(app.memory) },
    printTarget = { PrintTargetSetting(app) },
    updates = { UpdatesSetting(app) },
    debug = { DebugSetting(app) },
    hasCabinet = app.areas.cabinet != null
)

/**
 * Ширина раздела правее рельса.
 *
 * Рабочие столы — главный экран, чек, возврат, журнал — встают посередине
 * в пределах рабочего экрана: их строки на широком мониторе растягивались
 * на две тысячи точек, и сумма уезжала от названия. Формы и карточки —
 * деньги, кассиры, очередь, мастер, кабинет, настройки — занимают всё окно
 * правее рельса и на широком окне встают рядом: общий предел ширины
 * оставлял у них пустые полосы по обе стороны.
 */
fun Modifier.sectionWidth(section: Section): Modifier =
    if (section in WORK_TABLES) contentWidth(ContentKind.Workspace) else fillMaxWidth()

/** Разделы, которые встают посередине в пределах рабочего экрана. */
private val WORK_TABLES = setOf(Section.Dashboard, Section.Sale, Section.Returns, Section.History)
