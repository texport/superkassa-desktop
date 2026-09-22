package kz.mybrain.superkassa.desktop.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import kz.mybrain.superkassa.desktop.app.askWhereToSave
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogEntry
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.matching
import kz.mybrain.superkassa.desktop.ui.strings.DebugTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.debugTexts
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Look
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.SuperkassaTheme

/**
 * Окно журнала приложения.
 *
 * Стоит рядом с главным окном, а не внутри него: отлаживают по журналу
 * и по кассе одновременно — набирают чек и тут же смотрят, что ушло
 * к узлу. Вкладкой внутри кассы одно закрывало бы другое.
 *
 * Открывается вместе с режимом отладки и закрывается вместе с ним:
 * закрытое крестиком окно снимает и сам режим, иначе оно молча
 * не открылось бы после перезапуска.
 */
@Composable
fun LogWindow(language: Language, appearance: Appearance, look: Look, onClose: () -> Unit) {
    val texts = debugTexts(language)
    val state = rememberWindowState(size = DpSize(Sizes.logWindowWidth, Sizes.logWindowHeight))
    Window(onCloseRequest = onClose, title = texts.title, state = state) {
        SuperkassaTheme(appearance, look) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                LogBody(texts)
            }
        }
    }
}

/**
 * Содержимое окна: отбор сверху, строки посередине, обещание о тайном внизу.
 *
 * Отбор живёт этим окном, а не рабочим местом: его меняют по ходу разбора
 * несколько раз за минуту, и помнить последний выбор до следующего запуска
 * здесь не нужно.
 */
@Composable
private fun LogBody(texts: DebugTexts) {
    var level by remember { mutableStateOf(LogLevel.Debug) }
    var query by remember { mutableStateOf("") }
    val shown = AppLog.entries.matching(level, query)
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        LogFilters(
            texts = texts,
            level = level,
            query = query,
            shown = shown.size,
            onLevel = { level = it },
            onQuery = { query = it },
            onClear = AppLog::clear,
            onSave = { saveLog(shown, texts.save) }
        )
        LogLines(shown, texts, Modifier.weight(1f))
        Text(
            text = texts.secretsHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Сохраняет показанное в файл.
 *
 * Сохраняется то, что видно после отбора: в поддержку пересылают разбор
 * одного отказа, а не всю смену.
 */
private fun saveLog(entries: List<LogEntry>, title: String) {
    val target = askWhereToSave(SAVED_NAME, title) ?: return
    runCatching { target.writeText(entries.joinToString("\n") { it.line() }) }
}

/** Имя файла, предложенное при сохранении из окна. */
private const val SAVED_NAME = "superkassa-log.txt"
