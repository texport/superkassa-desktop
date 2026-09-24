package kz.mybrain.superkassa.presentation.common.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Подписанные строки сведений: подпись слева, значение справа.
 *
 * Один набор на все сводки настроек — ответ ОФД, сведения об узле,
 * данные авторизации. Подписи стоят по одной ширине, и значения
 * выстраиваются столбцом: их сверяют глазами, а не читают подряд.
 *
 * @param title подпись над строками; `null` — строки и так названы
 *   заголовком карточки, и вторая подпись тем же словом его повторяла бы.
 */
@Composable
fun FactLines(title: String?, rows: List<Pair<String, String>>, empty: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.inline)) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (rows.isEmpty()) {
            Text(empty, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        rows.forEach { (label, value) -> FactLine(label, value) }
    }
}

/** Подпись слева по одной ширине, значение справа. */
@Composable
private fun FactLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(Sizes.fieldForm)
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
