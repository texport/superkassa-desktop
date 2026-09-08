package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
 */
@Composable
fun MoreRow(more: Boolean, loading: Boolean, showMore: String, allShown: String, onMore: () -> Unit) {
    if (more) {
        TextButton(enabled = !loading, onClick = onMore) { Text(showMore) }
    } else {
        Text(
            text = allShown,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
