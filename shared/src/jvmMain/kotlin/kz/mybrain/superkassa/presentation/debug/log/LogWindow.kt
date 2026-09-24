package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.debug.debugTexts
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.SuperkassaTheme
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import kz.mybrain.superkassa.presentation.theme.size.Sizes

/**
 * Окно журнала приложения.
 *
 * Стоит рядом с главным окном, а не внутри него: отлаживают по журналу
 * и по кассе одновременно — набирают чек и тут же смотрят, что записано
 * о нём. Вкладкой внутри кассы одно закрывало бы другое.
 *
 * Открывается вместе с режимом отладки и закрывается вместе с ним:
 * закрытое крестиком окно снимает и сам режим, иначе оно молча
 * не открылось бы после перезапуска.
 */
@Composable
fun LogWindow(model: LogViewModel, language: Language, appearance: Appearance, look: Look, onClose: () -> Unit) {
    val texts = debugTexts(language)
    val state = rememberWindowState(size = DpSize(Sizes.logWindowWidth, Sizes.logWindowHeight))
    val journal by model.state.collectAsScreenState()
    Window(onCloseRequest = onClose, title = texts.title, state = state) {
        SuperkassaTheme(appearance, look) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                LogBody(journal, model, texts)
            }
        }
    }
}
