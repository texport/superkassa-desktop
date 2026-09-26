package kz.mybrain.superkassa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.Deferred
import kz.mybrain.superkassa.designsystem.adaptive.WindowClassRoot
import kz.mybrain.superkassa.designsystem.strings.ProvideStrings
import kz.mybrain.superkassa.designsystem.theme.SuperkassaTheme
import kz.mybrain.superkassa.domain.kassa.model.StartProblem
import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import kz.mybrain.superkassa.presentation.common.look.lookViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.strings.workplaceLanguage
import kz.mybrain.superkassa.presentation.debug.log.LogDialog
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.ShellScreen
import kz.mybrain.superkassa.presentation.shell.starting.StartRefusedScreen
import kz.mybrain.superkassa.presentation.shell.starting.StartingScreen

/**
 * Точка входа кассы на Android — единственная активность.
 *
 * Системная заставка держится до первого кадра, а не до готовности кассы:
 * ядро при открытии сверяет часы по сети, и это ожидание показывает общий
 * экран запуска с ходом, а не застывший значок. Касса открылась — окно
 * то же, что на компьютере: общий каркас с разделами. Не открылась —
 * экран с причиной и действием.
 *
 * Модели экранов живут в хранилище моделей активности и переживают
 * поворот экрана: корзина и набранное остаются на месте. Язык — язык
 * приложения: Android 13+ даёт выбрать его в настройках приложения
 * и пересоздаёт активность, а надписи берутся заново.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val opening = (application as SuperkassaApp).container
        setContent { KassaWindow(opening, onClose = ::finish) }
    }
}

/** Чем кончилось открытие кассы для окна. */
private sealed interface Start {
    data object Opening : Start

    class Opened(val app: AppContainer) : Start

    class Refused(val problem: StartProblem) : Start
}

/** Окно кассы: ожидание, отказ или каркас с разделами. */
@Composable
private fun KassaWindow(opening: Deferred<AppContainer>, onClose: () -> Unit) {
    val start by produceState<Start>(Start.Opening, opening) { value = opening.opened() }
    when (val now = start) {
        Start.Opening -> Bare { StartingScreen() }
        is Start.Refused -> Bare { StartRefusedScreen(now.problem, onClose) }
        is Start.Opened -> Window(now.app)
    }
}

/**
 * Ждёт кассу процесса.
 *
 * Вид окна перечитывается при каждом открытии активности: язык приложения
 * могли сменить в настройках системы, и активность пересоздана ради него.
 */
private suspend fun Deferred<AppContainer>.opened(): Start = runCatching { await() }.fold(
    onSuccess = { app -> Start.Opened(app.also { it.services.look.recall() }) },
    onFailure = { Start.Refused(StartProblem(StartRefusal.KassaNotOpened, it.javaClass.simpleName)) }
)

/** До кассы: тема системы и язык приложения — выбора кассира ещё не прочитать. */
@Composable
private fun Bare(content: @Composable () -> Unit) {
    SuperkassaTheme { ProvideStrings(workplaceLanguage(null), content) }
}

/** Тема, язык и класс окна — вокруг каркаса, как на компьютере. */
@Composable
private fun Window(app: AppContainer) {
    val look by lookViewModel(app.services.look).state.collectAsScreenState()
    SuperkassaTheme(look.appearance, look.look) {
        ProvideStrings(look.language) {
            // Клавиатура сдвигает окно, а не ложится поверх: иначе поля
            // «Получено» и «Пробить чек» внизу кассы уходили под неё.
            // Класс окна меряется по всему окну, до сдвига: иначе открытая
            // клавиатура делала планшет лёжа «низким» окном, рельс менялся
            // на полосу над клавиатурой, экран собирался заново и поле,
            // в которое начали печатать, теряло фокус.
            WindowClassRoot {
                Box(modifier = Modifier.fillMaxSize().imePadding()) { ShellScreen(app) }
            }
            // Журнал в режиме отладки — поверх кассы: соседнего окна здесь нет.
            LogDialog(app.services, app.areas.debug)
        }
    }
}
