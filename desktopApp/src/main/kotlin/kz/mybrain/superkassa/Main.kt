package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.texport.superkassa.embedded.api.Superkassa
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.designsystem.adaptive.WindowClassRoot
import kz.mybrain.superkassa.designsystem.keyboard.EscapeListener
import kz.mybrain.superkassa.designsystem.strings.ProvideStrings
import kz.mybrain.superkassa.designsystem.theme.SuperkassaTheme
import kz.mybrain.superkassa.designsystem.theme.motion.Durations
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.domain.kassa.model.StartProblem
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.presentation.common.look.LookUiState
import kz.mybrain.superkassa.presentation.common.look.lookViewModel
import kz.mybrain.superkassa.presentation.common.model.ProvideWindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowModels
import kz.mybrain.superkassa.presentation.debug.log.LogWindow
import kz.mybrain.superkassa.presentation.debug.log.logViewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.ShellScreen
import kz.mybrain.superkassa.presentation.shell.starting.StartRefusedScreen
import org.jetbrains.skia.Image
import kotlin.system.exitProcess

/**
 * Точка входа кассы для настольных систем.
 *
 * Касса работает в процессе приложения: ядро поднимается на каталоге данных
 * рабочего места, и все разделы говорят с ним напрямую. Данные прежнего
 * узла переносятся до кассы; не вышло — касса не открывается, а кассир
 * видит, почему и что делать. Что из чего собрано — в `Assembly.kt`.
 */
fun main() {
    System.setProperty("apple.awt.application.name", APP_NAME)
    // Журнал поднимается до первого обращения к кассе: иначе запуск,
    // ради разбора которого отладку и включали, в него не попадёт.
    AppLog.start()
    val preferences = Preferences()
    val look = WorkplaceLook(preferences)
    when (val start = startKassa(preferences)) {
        is KassaStart.Refused -> refused(start.problem, look)
        is KassaStart.Opened -> work(start.kassa, preferences, look)
    }
}

/** Касса открылась: рабочее окно, пока его не закроют. */
private fun work(kassa: Superkassa, preferences: Preferences, look: WorkplaceLook): Nothing {
    val models = WindowModels()
    val app = assemble(kassa, preferences, look)
    application(exitProcessOnExit = false) { SuperkassaApplication(app, preferences, models) }
    models.close()
    kassa.close()
    exitProcess(0)
}

/** Касса не открылась: окно с причиной, пока кассир его не закроет. */
private fun refused(problem: StartProblem, look: WorkplaceLook): Nothing {
    application(exitProcessOnExit = false) { RefusedWindow(problem, look) }
    exitProcess(1)
}

/** Имя бренда: строка меню, док и заголовок окна. */
internal const val APP_NAME = "Superkassa"

/** Значок окна: он же стоит в панели задач Windows и в переключателе окон. */
private const val APP_ICON = "icon.png"

/** Значок окна из ресурсов приложения. */
private fun appIcon(): Painter {
    val resource = KassaStart::class.java.classLoader.getResourceAsStream(APP_ICON)
    val bytes = checkNotNull(resource) { "$APP_ICON is missing" }.use { it.readBytes() }
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}

/** Окно отказа запуска — на языке и в теме, которые кассир выбрал раньше. */
@Composable
private fun ApplicationScope.RefusedWindow(problem: StartProblem, look: WorkplaceLook) {
    val shown = remember { LookUiState.of(look.state.value) }
    Window(onCloseRequest = ::exitApplication, title = APP_NAME, icon = remember { appIcon() }) {
        SuperkassaTheme(shown.appearance, shown.look) {
            ProvideStrings(shown.language) { StartRefusedScreen(problem, ::exitApplication) }
        }
    }
}

@Composable
private fun ApplicationScope.SuperkassaApplication(app: AppContainer, preferences: Preferences, models: WindowModels) {
    val windowState = rememberKassaWindow(preferences)
    RememberWindowSize(preferences, windowState)
    Window(
        onCloseRequest = ::exitApplication,
        title = APP_NAME,
        icon = remember { appIcon() },
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
        ProvideWindowModels(models) { WindowContent(app) }
    }
    ProvideWindowModels(models) { DebugWindow(app) }
}

/** Журнал — соседнее окно, а не раздел: по нему отлаживают главное, и одно не закрывает другое. */
@Composable
private fun DebugWindow(app: AppContainer) {
    val debugMode by AppLog.debugModes.collectAsState()
    if (debugMode) {
        val look by lookViewModel(app.services.look).state.collectAsState()
        val journal = logViewModel(app.services, app.areas.debug)
        LogWindow(journal, look.language, look.appearance, look.look) { journal.switchDebugMode(false) }
    }
}

/** Тема, язык и класс окна — вокруг каркаса. */
@Composable
private fun WindowContent(app: AppContainer) {
    val look by lookViewModel(app.services.look).state.collectAsState()
    SuperkassaTheme(look.appearance, look.look) {
        ProvideStrings(look.language) {
            // Окно меряется здесь, один раз: класс окна знают все
            // разделы, и никто не меряет его сам.
            WindowClassRoot { ShellScreen(app) }
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
private fun rememberKassaWindow(preferences: Preferences): WindowState = rememberWindowState(
    position = WindowPosition(Alignment.Center),
    size = remember {
        val wanted = preferences.windowSize
            ?: (Sizes.windowWidth.value.toInt() to Sizes.windowHeight.value.toInt())
        val (width, height) = fitToScreen(wanted, screenSize())
        DpSize(width.dp, height.dp)
    }
)

/**
 * Запоминает размер окна, когда рамку отпустили.
 *
 * Размер читается потоком снимков, а не ключом: ключом он подписывал на себя
 * весь состав приложения, а рамку тянут десятками движений в секунду.
 * Каждое движение — это запись файла настройки, и делать её по ходу
 * растягивания незачем.
 */
@Composable
private fun RememberWindowSize(preferences: Preferences, windowState: WindowState) {
    LaunchedEffect(Unit) {
        snapshotFlow { windowState.size }.collectLatest { size ->
            delay(Durations.afterTyping)
            val width = size.width.value.toInt()
            val height = size.height.value.toInt()
            if (width > 0 && height > 0) {
                preferences.windowSize = width to height
            }
        }
    }
}
