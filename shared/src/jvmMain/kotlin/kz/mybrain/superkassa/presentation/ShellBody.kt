package kz.mybrain.superkassa.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.cabinet.CabinetDocuments
import kz.mybrain.superkassa.presentation.cabinet.CabinetDoor
import kz.mybrain.superkassa.presentation.cabinet.CabinetScreen
import kz.mybrain.superkassa.presentation.cash.CashScreen
import kz.mybrain.superkassa.presentation.components.Chip
import kz.mybrain.superkassa.presentation.components.LanguagePicker
import kz.mybrain.superkassa.presentation.components.PrintOverlay
import kz.mybrain.superkassa.presentation.components.ThemeSwitch
import kz.mybrain.superkassa.presentation.dashboard.DashboardScreen
import kz.mybrain.superkassa.presentation.dashboard.dashboardViewModel
import kz.mybrain.superkassa.presentation.history.HistoryScreen
import kz.mybrain.superkassa.presentation.login.Door
import kz.mybrain.superkassa.presentation.login.LoginActions
import kz.mybrain.superkassa.presentation.login.LoginScreen
import kz.mybrain.superkassa.presentation.login.LoginUiState
import kz.mybrain.superkassa.presentation.queue.QueueScreen
import kz.mybrain.superkassa.presentation.returns.ReturnsScreen
import kz.mybrain.superkassa.presentation.sale.SaleScreen
import kz.mybrain.superkassa.presentation.session.CabinetSession
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.settings.SettingsScreen
import kz.mybrain.superkassa.presentation.settings.WorkplaceSettingsScreen
import kz.mybrain.superkassa.presentation.setup.ConnectKkmScreen
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.users.UsersScreen

/**
 * Содержимое выбранного раздела.
 *
 * Своего отступа у каркаса нет: поля разделов уже задают его одним
 * значением, и второй отступ поверх делал колонку содержимого шире
 * положенного.
 */
@Composable
internal fun SectionContent(
    session: Session,
    app: AppContainer,
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    section: Section
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Предел ширины — один на все разделы: на широком мониторе
        // экран не растягивается на две тысячи точек, а встаёт посередине.
        Box(modifier = Modifier.contentWidth(ContentKind.Workspace).fillMaxHeight()) {
            SectionScreen(session, app, cabinet, documents, section)
        }
        // Печатная форма живёт над всеми разделами: кассир открывает её
        // из журнала и вправе уйти в продажу, не теряя окна. То же окно
        // стоит и над дверью в кабинет — оно одно на оба входа.
        PrintOverlay(session)
    }
}

/** Экран выбранного раздела. */
@Composable
private fun SectionScreen(
    session: Session,
    app: AppContainer,
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    section: Section
) {
    when (section) {
        Section.Dashboard -> DashboardScreen(dashboardViewModel(app), session::previewDocument, session::printDocument)
        Section.Sale -> SaleScreen(session)
        Section.Returns -> ReturnsScreen(session)
        Section.Cash -> CashScreen(session)
        Section.History -> HistoryScreen(session)
        Section.Queue -> QueueScreen(session)
        Section.Users -> UsersScreen(session)
        Section.Register -> ConnectKkmScreen(session, cabinet)
        Section.Cabinet -> CabinetScreen(session, cabinet, documents)
        Section.Settings -> SettingsScreen(session)
    }
}

/** Вход кассира занимает окно целиком: пустые разделы без кассы ничему не учат. */
@Composable
internal fun SectionDoor(session: Session, cabinet: CabinetSession, state: LoginUiState, actions: LoginActions) {
    Box(modifier = Modifier.fillMaxSize()) {
        LoginScreen(
            state = state,
            actions = actions,
            header = { LoginExtras(session) },
            door = { door, close -> LoginDoor(session, cabinet, door, close) }
        )
        // Форму владелец может открыть и отсюда — через дверь в кабинет.
        PrintOverlay(session)
    }
}

/**
 * Что открыто за дверью входа.
 *
 * Заведение кассы, кабинет и настройки рабочего места — чужие входу
 * разделы; вход только открывает дверь, а что за ней, знает каркас.
 */
@Composable
private fun LoginDoor(session: Session, cabinet: CabinetSession, door: Door, close: () -> Unit) {
    when (door) {
        Door.Register -> ConnectKkmScreen(session, cabinet, close)
        Door.Cabinet -> CabinetDoor(session, cabinet, close)
        Door.Settings -> WorkplaceSettingsScreen(session, close)
        Door.Kkms -> Unit
    }
}

/** Рядом с названием входа: вид, язык и связь с узлом, на котором живут прочие разделы. */
@Composable
private fun RowScope.LoginExtras(session: Session) {
    val texts = LocalStrings.current
    ThemeSwitch(session)
    LanguagePicker(session)
    Chip(
        text = if (session.nodeAvailable) texts.common.nodeOnline else texts.common.nodeOffline,
        color = if (session.nodeAvailable) StatusColors.delivered else StatusColors.refused
    )
}
