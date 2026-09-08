package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Строка списка: главное слева, служебное под ним, сумма и состояние справа.
 *
 * Собрана на `ListItem` Material 3, а не своей строкой: у него уже
 * рассчитаны рост, поля и роли цвета для трёх уровней важности. Своя
 * разметка повторяла это числами и разъезжалась от списка к списку.
 *
 * Карточки под каждой строкой нет намеренно — тот же вывод, что и в
 * журнале кассы: сотня чеков в карточках растягивается на три экрана
 * прокрутки и рвёт столбец сумм зазорами. Соседние строки разделяет
 * подложка через одну.
 *
 * @param amount сумма строки; набирается денежным шрифтом, чтобы цифры
 *   в списке стояли столбцом.
 * @param support служебная часть строки, когда её мало одной строкой
 *   текста: у задачи очереди там причина отказа и время следующей попытки.
 * @param striped затенена ли строка: признак чередования, а не состояния.
 * @param selected выбрана ли строка в списке, у которого есть выбранное:
 *   она берёт вторичный контейнер схемы, как выделенный пункт по Material 3.
 */
@Composable
fun RecordRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    amount: String? = null,
    support: @Composable (() -> Unit)? = null,
    striped: Boolean = false,
    selected: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val opened = if (onClick == null) modifier else modifier.clickable(onClick = onClick)
    ListItem(
        modifier = opened.fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = rowBackground(striped, selected)),
        leadingContent = leading,
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = support ?: subtitle?.takeIf { it.isNotBlank() }?.let { note ->
            {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = { RowTail(amount, trailing) }
    )
}

/** Правый край строки: сумма и следом за ней состояние. */
@Composable
private fun RowTail(amount: String?, trailing: @Composable (() -> Unit)?) {
    if (amount == null && trailing == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (amount != null) {
            Text(text = amount, style = MoneyStyle.row)
        }
        trailing?.invoke()
    }
}

/**
 * Подложка строки.
 *
 * По умолчанию прозрачная, а не цвет поверхности: строка живёт внутри
 * карточки, и собственная поверхность закрыла бы её подложку своим
 * оттенком. Выбранная строка сильнее чередования: обе метки на одной
 * строке спорили бы за внимание.
 */
@Composable
private fun rowBackground(striped: Boolean, selected: Boolean): Color = when {
    selected -> MaterialTheme.colorScheme.secondaryContainer
    striped -> MaterialTheme.colorScheme.surfaceContainerLow
    else -> Color.Transparent
}
