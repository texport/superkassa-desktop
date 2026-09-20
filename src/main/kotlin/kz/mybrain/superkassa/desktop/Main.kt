package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kz.mybrain.superkassa.desktop.app.LocalNode
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.Shell
import kz.mybrain.superkassa.desktop.ui.debug.LogWindow
import kz.mybrain.superkassa.desktop.ui.strings.ProvideStrings
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.SuperkassaTheme

/**
 * Точка входа кассы для настольных систем.
 *
 * Приложение не хранит фискальных данных само: всё состояние живёт в узле
 * Суперкассы, который работает на этой же машине в режиме DESKTOP. Так
 * приложение можно закрыть посреди смены, ничего не потеряв.
 */
fun main() {
    System.setProperty("apple.awt.application.name", APP_NAME)
    // Узел поднимается до окна: установщик несёт его с собой, и кассиру
    // не из чего понять, что программ на самом деле две. Окно ждёт ответа
    // узла — иначе список касс встретит пустотой, пока тот загружается.
    // При разработке узла в ресурсах нет, и ожидания тоже.
    val node = LocalNode(Preferences().nodeUrl)
    if (node.start()) node.awaitReady()
    application { SuperkassaApplication() }
}

/** Имя бренда: строка меню, док и заголовок окна. */
private const val APP_NAME = "Superkassa"

@Composable
private fun ApplicationScope.SuperkassaApplication() {
    // Адрес узла читается из настроек при каждом обращении: его меняют
    // с экрана входа, и перезапуск ради этого не нужен.
    val session = remember {
        // Журнал поднимается до первого обращения к узлу: иначе запуск,
        // ради разбора которого отладку и включали, в него не попадёт.
        AppLog.start()
        val preferences = Preferences()
        Session(ServerClient(address = { preferences.nodeUrl }), preferences)
    }
    // Размер окна берётся тот, каким кассир оставил его в прошлый раз;
    // при первом запуске — подобранный под ноутбучный экран.
    val remembered = remember { session.rememberedWindowSize }
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = remembered
            ?.let { (width, height) -> DpSize(width.dp, height.dp) }
            ?: DpSize(Sizes.windowWidth, Sizes.windowHeight)
    )
    LaunchedEffect(windowState.size) {
        val width = windowState.size.width.value.toInt()
        val height = windowState.size.height.value.toInt()
        if (width > 0 && height > 0) {
            session.rememberWindowSize(width, height)
        }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = APP_NAME,
        state = windowState
    ) {
        SuperkassaTheme(session.appearance) {
            ProvideStrings(session.language) {
                Shell(session)
            }
        }
    }
    // Журнал — соседнее окно, а не раздел кассы: по нему отлаживают
    // то, что делают в главном окне, и одно не должно закрывать другое.
    if (AppLog.debugMode) {
        LogWindow(session.language, session.appearance) { AppLog.switchDebugMode(false) }
    }
}
