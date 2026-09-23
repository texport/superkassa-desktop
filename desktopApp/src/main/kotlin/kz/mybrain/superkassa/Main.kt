package kz.mybrain.superkassa

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
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.log.NodeOutput
import kz.mybrain.superkassa.data.node.LocalNode
import kz.mybrain.superkassa.data.node.NodeHandover
import kz.mybrain.superkassa.presentation.ProvideWindowModels
import kz.mybrain.superkassa.presentation.Shell
import kz.mybrain.superkassa.presentation.WindowModels
import kz.mybrain.superkassa.presentation.adaptive.WindowClassRoot
import kz.mybrain.superkassa.presentation.components.EscapeListener
import kz.mybrain.superkassa.presentation.debug.LogWindow
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.session.appearance
import kz.mybrain.superkassa.presentation.session.look
import kz.mybrain.superkassa.presentation.session.rememberWindowSize
import kz.mybrain.superkassa.presentation.session.rememberedWindowSize
import kz.mybrain.superkassa.presentation.strings.ProvideStrings
import kz.mybrain.superkassa.presentation.theme.Durations
import kz.mybrain.superkassa.presentation.theme.Sizes
import kz.mybrain.superkassa.presentation.theme.SuperkassaTheme
import kotlin.system.exitProcess

/**
 * Точка входа кассы для настольных систем.
 *
 * Касса работает в процессе приложения: ядро поднимается на каталоге данных
 * рабочего места, и вход с главным экраном говорят с ним напрямую. Прочие
 * разделы пока говорят с узлом — он поднимается рядом, как и раньше, и уйдёт,
 * когда разделы переведут на ядро. Что из чего собрано — в `Assembly.kt`.
 */
fun main() {
    System.setProperty("apple.awt.application.name", APP_NAME)
    // Журнал поднимается до первого обращения к кассе и к узлу: иначе запуск,
    // ради разбора которого отладку и включали, в него не попадёт.
    AppLog.start()
    val preferences = Preferences()
    // Перенос данных узла — до кассы и до узла: после него узел не нужен.
    val handover = handOverNode(preferences).getOrElse { exitProcess(1) }
    val kassa = openKassa(preferences) ?: exitProcess(1)
    // Узел поднимается до окна: установщик несёт его с собой. Окно ждёт
    // ответа узла — иначе разделы на узле встретят пустотой, пока тот
    // загружается. При разработке узла в ресурсах нет, и ожидания тоже.
    if (!NodeHandover.retired(handover)) {
        val node = LocalNode(preferences.nodeUrl)
        if (node.start()) node.awaitReady()
    }
    val models = WindowModels()
    application(exitProcessOnExit = false) { SuperkassaApplication(assemble(kassa, preferences), models) }
    models.close()
    kassa.close()
    exitProcess(0)
}

/** Имя бренда: строка меню, док и заголовок окна. */
internal const val APP_NAME = "Superkassa"

/** Значок окна: он же стоит в панели задач Windows и в переключателе окон. */
private const val APP_ICON = "icon.png"

@Composable
private fun ApplicationScope.SuperkassaApplication(assembly: Assembly, models: WindowModels) {
    val session = assembly.session
    val windowState = rememberKassaWindow(session)
    FollowNodeOutput()
    RememberWindowSize(session, windowState)
    Window(
        onCloseRequest = ::exitApplication,
        title = APP_NAME,
        icon = painterResource(APP_ICON),
        state = windowState
    ) {
        // Нижняя граница размера — у самого окна, а не у разметки: система
        // разрешала ужать окно до полосы, в которой не помещается ни одна
        // колонка, и кассир получал экран из обрезков вместо рабочего места.
        LaunchedEffect(Unit) {
            window.minimumSize = windowMinimum(Sizes.windowMinWidth, Sizes.windowMinHeight)
        }
        // Escape слушается ниже Compose, у самого окна: наложения живут
        // своим слоем и клавиша до разделов не доходит, а до чего доходит —
        // решает фокус. Кто закрывается — в [EscapeCloses].
        EscapeListener()
        WindowContent(assembly, models)
    }
    // Журнал — соседнее окно, а не раздел кассы: по нему отлаживают
    // то, что делают в главном окне, и одно не должно закрывать другое.
    if (AppLog.debugMode) {
        LogWindow(session.language, session.appearance, session.look) { AppLog.switchDebugMode(false) }
    }
}

/** Тема, язык, класс окна и модели окна — вокруг каркаса. */
@Composable
private fun WindowContent(assembly: Assembly, models: WindowModels) {
    val session = assembly.session
    SuperkassaTheme(session.appearance, session.look) {
        ProvideStrings(session.language) {
            // Окно меряется здесь, один раз: класс окна знают все
            // разделы, и никто не меряет его сам.
            WindowClassRoot {
                ProvideWindowModels(models) { Shell(session, assembly.app) }
            }
        }
    }
}

/**
 * Окно того размера, каким кассир оставил его в прошлый раз.
 *
 * При первом запуске — подобранного под ноутбучный экран. И тот и другой
 * ужимаются до экрана этой машины: запомненный на внешнем мониторе
 * уезжал за край ноутбука вместе с шапкой.
 */
@Composable
private fun rememberKassaWindow(session: Session): WindowState = rememberWindowState(
    position = WindowPosition(Alignment.Center),
    size = remember {
        val wanted = session.rememberedWindowSize
            ?: (Sizes.windowWidth.value.toInt() to Sizes.windowHeight.value.toInt())
        val (width, height) = fitToScreen(wanted, screenSize())
        DpSize(width.dp, height.dp)
    }
)

/**
 * В режиме отладки вывод узла дочитывается в тот же журнал: иначе
 * цепочка обрывается на границе с ним, а обмен с ОФД идёт там.
 */
@Composable
private fun FollowNodeOutput() {
    LaunchedEffect(AppLog.debugMode) {
        if (!AppLog.debugMode) return@LaunchedEffect
        NodeOutput(LocalNode.output(), AppLog.journal).follow()
    }
}

/**
 * Запоминает размер окна, когда рамку отпустили.
 *
 * Размер читается потоком снимков, а не ключом: ключом он подписывал на себя
 * весь состав приложения, а рамку тянут десятками движений в секунду.
 * Каждое движение — это запись файла настройки, и делать её по ходу
 * растягивания незачем.
 */
@Composable
private fun RememberWindowSize(session: Session, windowState: WindowState) {
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
}
