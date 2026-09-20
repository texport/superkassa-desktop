package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

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

/** Подпись столбца. */
@Composable
internal fun HeadCell(title: String, modifier: Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Значение в строке. */
@Composable
internal fun RowCell(value: String, modifier: Modifier) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
