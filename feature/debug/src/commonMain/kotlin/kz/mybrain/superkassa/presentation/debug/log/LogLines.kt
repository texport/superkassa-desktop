package kz.mybrain.superkassa.presentation.debug.log

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
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.list.ScrollableList
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.LogStyle
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.presentation.words.debug.name
import kz.mybrain.superkassa.strings.api.debug.DebugTexts

/**
 * Строки журнала списком.
 *
 * Список едет за свежей записью сам: отлаживают по тому, что происходит
 * сейчас, и догонять его прокруткой при каждом обращении к кассе нельзя.
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.inline)) {
        LogHead(entry, texts)
        entry.body?.let { body ->
            Text(
                text = body,
                style = LogStyle.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.blockPadding)
            )
        }
    }
}

/**
 * Шапка строки: время, уровень, источник и текст записи.
 *
 * В окне шире телефона — колонками: уровень и источник стоят ровно друг
 * под другом, и глаз бежит по ним вниз. На телефоне колонки постоянной
 * ширины съедали строку, и текст записи вытягивался в столбик по слову:
 * там время, уровень и источник стоят строкой над текстом.
 */
@Composable
private fun LogHead(entry: LogEntry, texts: DebugTexts) {
    if (LocalWindowClass.current.width == WidthClass.Compact) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap)) { LogMarks(entry, texts, Modifier) }
            Text(text = entry.text, style = LogStyle.line)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap)) {
            LogMarks(entry, texts, Modifier.width(Sizes.logLevelColumn), Modifier.width(Sizes.logSourceColumn))
            Text(text = entry.text, style = LogStyle.line)
        }
    }
}

/** Время, уровень и источник записи; ширины колонок задаёт строка. */
@Composable
private fun LogMarks(entry: LogEntry, texts: DebugTexts, level: Modifier, source: Modifier = level) {
    Text(text = entry.time, style = LogStyle.line, color = MaterialTheme.colorScheme.outline)
    Text(text = texts.name(entry.level), style = LogStyle.line, color = colorOf(entry.level), modifier = level)
    Text(
        text = texts.name(entry.source),
        style = LogStyle.line,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = source
    )
}

/** Цвет уровня: роль схемы по смыслу, а не свой оттенок. */
@Composable
private fun colorOf(level: LogLevel): Color = when (level) {
    LogLevel.Debug -> MaterialTheme.colorScheme.outline
    LogLevel.Info -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.Warning -> StatusColors.pending
    LogLevel.Failure -> MaterialTheme.colorScheme.error
}
