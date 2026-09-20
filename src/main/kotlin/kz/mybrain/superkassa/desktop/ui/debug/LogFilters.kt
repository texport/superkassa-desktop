package kz.mybrain.superkassa.desktop.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.SearchField
import kz.mybrain.superkassa.desktop.ui.strings.DebugTexts
import kz.mybrain.superkassa.desktop.ui.strings.name
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Отбор строк журнала и действия над ними.
 *
 * Уровень выбирается сегментами: значений четыре, и все они нужны под
 * рукой — от «покажи всё» до «покажи только отказы». Поиск идёт по тексту
 * строки и по телу запроса: отказ ищут по пути обращения или по коду.
 *
 * Главное действие окна — сохранить показанное в файл, и только оно
 * залито; «очистить» стоит рядом текстовой кнопкой.
 *
 * Ряд переносится, а не сжимается. Строкой он был `Row` с распоркой,
 * и в узком окне разметка отбирала ширину у всего подряд: счётчик
 * «Строк: 160» вставал столбиком по одному знаку, поле поиска сжималось
 * до двух строк, а кнопки уходили за край. `FlowRow` переносит то, что
 * не поместилось, целым элементом — окно журнала открывают любой ширины.
 *
 * @param shown сколько строк осталось после отбора.
 */
@Composable
internal fun LogFilters(
    texts: DebugTexts,
    level: LogLevel,
    query: String,
    shown: Int,
    onLevel: (LogLevel) -> Unit,
    onQuery: (String) -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        ChoiceSegments(
            options = LogLevel.entries,
            selected = level,
            label = { texts.name(it) },
            onSelect = onLevel
        )
        SearchField(
            value = query,
            label = texts.search,
            onChange = onQuery,
            modifier = Modifier.widthIn(max = Sizes.fieldSearch)
        )
        LogActions(texts, shown, onClear, onSave)
    }
}

/**
 * Сколько строк осталось после отбора и что с ними можно сделать.
 *
 * Счётчик и обе кнопки стоят одним рядом и переносятся вместе: между
 * собой они не разрываются, потому что читаются как одно действие над
 * показанным. Счётчик набран в одну строку — переносить «Строк: 160»
 * по знакам нечего.
 */
@Composable
private fun LogActions(texts: DebugTexts, shown: Int, onClear: () -> Unit, onSave: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogShownCount(texts, shown)
        TextButton(onClick = onClear) { Text(texts.clear) }
        FilledTonalButton(onClick = onSave) { Text(texts.save) }
    }
}

/**
 * Сколько строк осталось после отбора.
 *
 * Строка одна и переносу не подлежит: «Строк: 160» в узком окне вставало
 * столбиком по одному знаку — разметка отбирала у счётчика ширину, а он
 * покорно её принимал.
 */
@Composable
internal fun LogShownCount(texts: DebugTexts, shown: Int) {
    Text(
        text = "${texts.lines}: $shown",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false
    )
}
