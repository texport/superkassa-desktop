package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kz.mybrain.superkassa.desktop.app.LocalNode
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.appearance
import kz.mybrain.superkassa.desktop.app.fitToScreen
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.NodeOutput
import kz.mybrain.superkassa.desktop.app.look
import kz.mybrain.superkassa.desktop.app.rememberWindowSize
import kz.mybrain.superkassa.desktop.app.rememberedWindowSize
import kz.mybrain.superkassa.desktop.app.screenSize
import kz.mybrain.superkassa.desktop.app.windowMinimum
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.Shell
import kz.mybrain.superkassa.desktop.ui.components.EscapeListener
import kz.mybrain.superkassa.desktop.ui.debug.LogWindow
import kz.mybrain.superkassa.desktop.ui.strings.ProvideStrings
import kz.mybrain.superkassa.desktop.ui.theme.Durations
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

/** Значок окна: он же стоит в панели задач Windows и в переключателе окон. */
private const val APP_ICON = "icon.png"

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
    // при первом запуске — подобранный под ноутбучный экран. И тот и другой
    // ужимаются до экрана этой машины: запомненный на внешнем мониторе
    // уезжал за край ноутбука вместе с шапкой.
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = remember {
            val wanted = session.rememberedWindowSize
                ?: (Sizes.windowWidth.value.toInt() to Sizes.windowHeight.value.toInt())
            val (width, height) = fitToScreen(wanted, screenSize())
            DpSize(width.dp, height.dp)
        }
    )
    // В режиме отладки вывод узла дочитывается в тот же журнал: иначе
    // цепочка обрывается на границе с ним, а обмен с ОФД идёт там.
    LaunchedEffect(AppLog.debugMode) {
        if (!AppLog.debugMode) return@LaunchedEffect
        NodeOutput(LocalNode.output(), AppLog.journal).follow()
    }
    // Размер окна читается потоком снимков, а не ключом этого места.
    // Ключом он подписывал на себя весь состав приложения: каждое движение
    // рамки заново собирало окно вместе с темой и словарями, а рамку тянут
    // десятками движений в секунду.
    //
    // Записывается размер, когда рамку отпустили: каждое движение — это
    // запись файла настройки на диск, и делать её по ходу растягивания
    // незачем.
    LaunchedEffect(Unit) {
        snapshotFlow { windowState.size }.collectLatest { size ->
            delay(Durations.afterTyping)
            val width = size.width.value.toInt()
            val height = size.height.value.toInt()
            if (width > 0 && height > 0) {
                session.rememberWindowSize(width, height)
            }
        }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = APP_NAME,
        icon = painterResource(APP_ICON),
        state = windowState
    ) {
        // Нижняя граница размера — у самого окна, а не у разметки: system
        // разрешала ужать окно до полосы, в которой не помещается ни одна
        // колонка, и кассир получал экран из обрезков вместо рабочего места.
        LaunchedEffect(Unit) {
            window.minimumSize = windowMinimum(Sizes.windowMinWidth, Sizes.windowMinHeight)
        }
        // Escape слушается ниже Compose, у самого окна: наложения живут
        // своим слоем и клавиша до разделов не доходит, а до чего доходит —
        // решает фокус. Кто закрывается — в [EscapeCloses].
        EscapeListener()
        SuperkassaTheme(session.appearance, session.look) {
            ProvideStrings(session.language) {
                Shell(session)
            }
        }
    }
    // Журнал — соседнее окно, а не раздел кассы: по нему отлаживают
    // то, что делают в главном окне, и одно не должно закрывать другое.
    if (AppLog.debugMode) {
        LogWindow(session.language, session.appearance, session.look) { AppLog.switchDebugMode(false) }
    }
}
