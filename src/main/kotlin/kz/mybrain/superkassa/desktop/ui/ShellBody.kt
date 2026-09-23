package kz.mybrain.superkassa.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.adaptive.ContentKind
import kz.mybrain.superkassa.desktop.ui.adaptive.contentWidth
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetScreen
import kz.mybrain.superkassa.desktop.ui.cash.CashScreen
import kz.mybrain.superkassa.desktop.ui.components.PrintOverlay
import kz.mybrain.superkassa.desktop.ui.dashboard.DashboardScreen
import kz.mybrain.superkassa.desktop.ui.history.HistoryScreen
import kz.mybrain.superkassa.desktop.ui.login.LoginScreen
import kz.mybrain.superkassa.desktop.ui.login.LoginState
import kz.mybrain.superkassa.desktop.ui.queue.QueueScreen
import kz.mybrain.superkassa.desktop.ui.returns.ReturnsScreen
import kz.mybrain.superkassa.desktop.ui.sale.SaleScreen
import kz.mybrain.superkassa.desktop.ui.settings.SettingsScreen
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import kz.mybrain.superkassa.desktop.ui.users.UsersScreen

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
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    section: Section
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Предел ширины — один на все разделы: на широком мониторе
        // экран не растягивается на две тысячи точек, а встаёт посередине.
        Box(modifier = Modifier.contentWidth(ContentKind.Workspace).fillMaxHeight()) {
            SectionScreen(session, cabinet, documents, section)
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
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    section: Section
) {
    when (section) {
        Section.Dashboard -> DashboardScreen(session)
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
internal fun SectionDoor(session: Session, cabinet: CabinetSession, door: LoginState) {
    Box(modifier = Modifier.fillMaxSize()) {
        LoginScreen(session, cabinet, door)
        // Форму владелец может открыть и отсюда — через дверь в кабинет.
        PrintOverlay(session)
    }
}
