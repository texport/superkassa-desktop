package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Клетки таблиц аналитики.
 *
 * Заведены один раз на раздел: столбцами здесь идут и адреса обмена,
 * и сводка по кассам, и сводка по точкам. Написанные трижды, они
 * разъехались бы шрифтом подписи и обрезкой длинного названия — а
 * читают эти таблицы сверху вниз, сравнивая строки между собой.
 *
 * Ширину клетка не задаёт: её назначает таблица, потому что только она
 * знает, какой столбец тянется, а какой стоит на месте.
 */

/**
 * Подпись столбца.
 *
 * @param lines сколько строк ей отведено. Одна — там, где столбец шире
 *   подписи; две нужны узким столбцам: «Заявление в КГД» в сотню точек
 *   одной строкой обрывается на первом же слове.
 */
@Composable
internal fun HeadCell(title: String, modifier: Modifier, lines: Int = 1) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Значение в строке.
 *
 * @param tone цвет значения там, где он несёт смысл: столбец отказов
 *   учёта находят глазом по цвету, а не по подписи над ним. Не задан —
 *   клетка берёт цвет содержимого, как и весь остальной текст таблицы.
 */
@Composable
internal fun RowCell(value: String, modifier: Modifier, tone: Color = Color.Unspecified) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = tone,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Строка таблицы аналитики.
 *
 * Одна и та же у заголовка и у значений: набранные порознь, они
 * разъезжались зазором между столбцами, и подпись переставала стоять
 * над своим числом.
 */
@Composable
internal fun TableRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
