package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Низ страничного списка: «показать ещё» или строка о том, что показано всё.
 *
 * Молчание под списком не отличает «всё» от «оборвалось на двухсотой»,
 * поэтому строка стоит и тогда, когда показывать больше нечего.
 *
 * Заведено один раз: такой низ есть у журнала дня, у прошлых смен и
 * у документов кабинета. Написанный трижды, он разъезжался — где-то
 * кнопка гасла на время загрузки, где-то нет.
 *
 * @param more есть ли ещё страницы.
 * @param loading идёт ли чтение: кнопка на это время гаснет, чтобы
 *   нажатие не заказало ту же страницу дважды.
 * @param note сколько строк показано из скольких — словами и рядом
 *   с кнопкой, которая это меняет. В заголовке раздела то же число
 *   читалось как оторванная от всего цифра.
 */
@Composable
fun MoreRow(
    more: Boolean,
    loading: Boolean,
    showMore: String,
    allShown: String,
    note: String? = null,
    onMore: () -> Unit
) {
    if (!more) {
        Footnote(allShown)
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(enabled = !loading, onClick = onMore) { Text(showMore) }
        if (!note.isNullOrBlank()) Footnote(note)
    }
}

/** Служебная строка под списком. */
@Composable
private fun Footnote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
