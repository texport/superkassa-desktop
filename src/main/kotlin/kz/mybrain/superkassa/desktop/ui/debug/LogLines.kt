package kz.mybrain.superkassa.desktop.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.app.log.LogEntry
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.DebugTexts
import kz.mybrain.superkassa.desktop.ui.strings.name
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.LogStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Строки журнала списком.
 *
 * Список едет за свежей записью сам: отлаживают по тому, что происходит
 * сейчас, и догонять его прокруткой при каждом обращении к узлу нельзя.
 */
@Composable
internal fun LogLines(entries: List<LogEntry>, texts: DebugTexts, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) {
        EmptyState(
            icon = AppIcons.debug,
            title = texts.empty,
            hint = texts.emptyHint,
            modifier = modifier,
            centered = true
        )
        return
    }
    val state = rememberLazyListState()
    LaunchedEffect(entries.size) { state.scrollToItem(entries.lastIndex) }
    ScrollableList(modifier = modifier, state = state) {
        items(entries) { entry -> LogRow(entry, texts) }
    }
}

/**
 * Одна строка: время, уровень, источник и текст, а под ними — тело.
 *
 * Уровень и источник стоят своими столбцами заданной ширины: журнал
 * читают сверху вниз по одному столбцу, а не по каждой строке заново.
 */
@Composable
private fun LogRow(entry: LogEntry, texts: DebugTexts) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.hairline)) {
        LogHead(entry, texts)
        entry.body?.let { body ->
            Text(
                text = body,
                style = LogStyle.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.roomy)
            )
        }
    }
}

/** Шапка строки: время, уровень, источник и текст записи. */
@Composable
private fun LogHead(entry: LogEntry, texts: DebugTexts) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Text(
            text = entry.time(),
            style = LogStyle.line,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = texts.name(entry.level),
            style = LogStyle.line,
            color = colorOf(entry.level),
            modifier = Modifier.width(Sizes.logLevelColumn)
        )
        Text(
            text = texts.name(entry.source),
            style = LogStyle.line,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(Sizes.logSourceColumn)
        )
        Text(text = entry.text, style = LogStyle.line)
    }
}

/** Цвет уровня: роль схемы по смыслу, а не свой оттенок. */
@Composable
private fun colorOf(level: LogLevel): Color = when (level) {
    LogLevel.Debug -> MaterialTheme.colorScheme.outline
    LogLevel.Info -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.Warning -> StatusColors.pending
    LogLevel.Failure -> MaterialTheme.colorScheme.error
}
