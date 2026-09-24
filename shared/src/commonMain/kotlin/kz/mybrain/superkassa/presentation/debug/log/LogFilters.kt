package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.foundation.layout.Arrangement
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
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.field.SearchField
import kz.mybrain.superkassa.presentation.common.picker.ChoiceSegments
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.words.debug.name
import kz.mybrain.superkassa.strings.api.debug.DebugTexts

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
 */
@Composable
internal fun LogFilters(texts: DebugTexts, journal: LogUiState, actions: LogActions) {
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.fieldGap) {
        ChoiceSegments(
            options = LogLevel.entries,
            selected = journal.filter,
            label = { texts.name(it) },
            onSelect = actions::filter
        )
        SearchField(
            value = journal.query,
            label = texts.search,
            onChange = actions::search,
            // Поиск берёт остаток строки и не бывает уже своей подписи:
            // пределом сверху в 280 точек казахская подпись «Жол бойынша
            // іздеу» ломалась в две строки, а справа пустовала половина окна.
            modifier = Modifier.weight(1f).widthIn(min = Sizes.fieldSearch)
        )
        LogCommands(texts, journal.shown.size, actions)
    }
}

/**
 * Сколько строк осталось после отбора и что с ними можно сделать.
 *
 * Счётчик и обе кнопки стоят одним рядом и переносятся вместе: между
 * собой они не разрываются, потому что читаются как одно действие над
 * показанным. Счётчик набран в одну строку — переносить «Строк: 160»
 * по знакам нечего.
 *
 * Сохранение открывает окно выбора файла системы: на компьютере — окно
 * «Сохранить как», на Android — системное окно «Сохранить».
 */
@Composable
private fun LogCommands(texts: DebugTexts, shown: Int, actions: LogActions) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogShownCount(texts, shown)
        TextButton(onClick = actions::clear) { Text(texts.clear) }
        FilledTonalButton(onClick = actions::save) { Text(texts.save) }
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
