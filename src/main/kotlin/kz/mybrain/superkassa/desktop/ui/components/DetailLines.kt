package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Строки «подпись — значение», одни на всё приложение.
 *
 * Такие строки есть в итогах чека, в карточке кассы, в чеке кабинета
 * и в журналах. Написанные заново в каждом месте, они разъезжались:
 * где-то сумма шла обычным шрифтом, где-то подпись была ярче значения.
 *
 * Денежные строки набраны шрифтом [MoneyStyle]: цифры выравниваются
 * столбцом, и суммы читаются друг под другом.
 */
@Composable
fun DetailLine(title: String, value: String?) {
    if (value.isNullOrBlank()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Слагаемое суммы: подпись слева, деньги справа, оба приглушены. */
@Composable
fun MinorSumLine(title: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = amount,
            style = MoneyStyle.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Главная сумма экрана: подпись мелко, число крупно. */
@Composable
fun HeroSumLine(title: String, amount: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(text = amount, style = MoneyStyle.hero, color = color, modifier = Modifier.fillMaxWidth())
}

/** Строка списка: название слева, сумма справа в денежном столбце. */
@Composable
fun NamedSumRow(name: String, note: String? = null, amount: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (!note.isNullOrBlank()) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = amount, style = MoneyStyle.row)
    }
}
