package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Карточка раздела: заголовок, дополнение справа и содержимое под ними.
 *
 * Раздел из карточки, столбца, поля и заголовка набирался на каждом экране
 * заново, и они разъехались: где-то заголовок стоял под содержимым, где-то
 * внутреннее поле было вдвое уже. Здесь эта раскладка объявлена один раз.
 *
 * @param count сколько строк в разделе. Стоит вплотную к названию
 *   и приглушённой подписью: голое число у правого края читалось как
 *   оторванная от всего цифра, а к названию оно и относится.
 * @param trailing то, что стоит в строке заголовка справа: состояние
 *   или объяснение раздела.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                SectionTitle(title, count, Modifier.weight(1f))
                trailing()
            }
            content()
        }
    }
}

/**
 * Карточка раздела, которую можно свернуть.
 *
 * Отличается от [SectionCard] только стрелкой в заголовке: длинная
 * карточка собирается из таких разделов, и свёрнутое остаётся на экране
 * строкой заголовка, а не исчезает без следа.
 */
@Composable
fun CollapsibleCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    count: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.normal)) {
            CollapsibleSection(
                title = title,
                expanded = expanded,
                onToggle = onToggle,
                count = count,
                trailing = trailing,
                content = content
            )
        }
    }
}

/**
 * Название раздела и, если есть, счётчик строк рядом с ним.
 *
 * Счётчик набран подписью в приглушённом цвете и стоит сразу за
 * названием: он поясняет название, а не спорит с ним за внимание.
 * Правый край строки заголовка оставлен состоянию и действиям.
 */
@Composable
fun SectionTitle(title: String, count: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (!count.isNullOrBlank()) {
            Text(
                text = count,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
