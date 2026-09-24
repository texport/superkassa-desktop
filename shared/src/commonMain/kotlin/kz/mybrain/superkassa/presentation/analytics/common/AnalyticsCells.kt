package kz.mybrain.superkassa.presentation.analytics.common

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.text.MoneyText
import kz.mybrain.superkassa.presentation.common.text.NumberText
import kz.mybrain.superkassa.presentation.theme.size.NumberStyle

/**
 * Клетки таблиц аналитики.
 *
 * Заведены один раз на раздел: столбцами здесь идут и адреса обмена,
 * и сводка по кассам, и сводка по точкам. Написанные трижды, они
 * разъехались бы шрифтом подписи и обрезкой длинного названия — а
 * читают эти таблицы сверху вниз, сравнивая строки между собой.
 *
 * Ширину клетка не задаёт: её назначает таблица ([TableAcross]
 * или [kz.mybrain.superkassa.presentation.common.table.ScrollingTable]), потому
 * что только она знает, какой столбец тянется, а какой стоит на месте.
 */

/**
 * Подпись столбца.
 *
 * @param lines сколько строк ей отведено. Двух хватает почти везде:
 *   «Последняя связь» над столбцом времени встаёт по словам, а не
 *   обрывается многоточием; самым узким столбцам нужно больше.
 * @param numeric подпись над числами стоит у правого края, над ними.
 */
@Composable
internal fun HeadCell(title: String, lines: Int = HEAD_LINES, numeric: Boolean = false) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (numeric) TextAlign.End else TextAlign.Start
    )
}

/**
 * Слово в строке: название, адрес, время.
 *
 * Тем же кеглем, что и числа строки ([NumberStyle.cell]): строка таблицы
 * набирается одним кеглем, а не двумя. Многоточие допустимо у слова,
 * у числа — никогда.
 *
 * @param tone цвет значения там, где он несёт смысл: столбец отказов
 *   учёта находят глазом по цвету, а не по подписи над ним. Не задан —
 *   клетка берёт цвет содержимого, как и весь остальной текст таблицы.
 */
@Composable
internal fun RowCell(value: String, tone: Color = Color.Unspecified) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = tone,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Счёт в строке: целиком, с разрядами, моноширинно и вправо. */
@Composable
internal fun CountCell(value: Int, tone: Color = LocalContentColor.current) {
    NumberText(Money.count(value), color = tone)
}

/** Сумма в строке: целиком и тем же кеглем, что и остальные числа строки. */
@Composable
internal fun SumCell(text: String) {
    MoneyText(text, style = NumberStyle.cell)
}

/** Строк у подписи столбца по умолчанию. */
private const val HEAD_LINES = 2
