package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Строка под блоком экрана: помеха — или подсказка, когда помех нет.
 *
 * Место под строкой занято всегда, даже когда сказать нечего: иначе
 * появление ошибки сдвигает кнопку под рукой кассира, и нажатие уходит
 * не туда.
 */
@Composable
fun Hint(problem: String?, hint: String?) {
    Text(
        text = problem ?: hint.orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = if (problem != null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}
