package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.AppTopBar
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Журнал приложения на Android — во весь экран поверх кассы.
 *
 * Соседнего окна, как на компьютере, здесь нет, поэтому журнал встаёт
 * поверх разделов, пока включён режим отладки. Закрыть его — стрелкой
 * или жестом «назад» — значит выключить режим: иначе журнал вставал бы
 * снова при каждом открытии кассы.
 */
@Composable
fun LogDialog(app: AppContainer) {
    val model = logViewModel(app)
    val journal by model.state.collectAsScreenState()
    if (!journal.book.debugMode) return
    val texts = textsOf(LocalLanguage.current).debug
    val close = { model.switchDebugMode(false) }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column {
                AppTopBar(title = texts.title, onBack = close, backLabel = LocalStrings.current.preview.close) {}
                LogBody(journal, model, texts)
            }
        }
    }
}
