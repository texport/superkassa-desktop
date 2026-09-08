package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Шапка окна — одна на всё приложение.
 *
 * У кассы она называет кассу и её состояние, у кабинета — компанию
 * и вошедшего владельца. Прежде у кабинета была своя шапка внутри
 * рабочей области: две шапки друг под другом, разной высоты и с разными
 * полями, а язык и выход у них лежали в разных местах окна.
 *
 * Заголовок и действия отступают от краёв окна теми же полями, что
 * и содержимое разделов: иначе название начинается от самого края стекла.
 *
 * @param badge необязательный опознавательный значок слева.
 * @param actions кнопки справа: у каждого раздела свои.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    badge: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        navigationIcon = { badge?.let { TopBarBadge(it) } },
        title = {
            Column(modifier = Modifier.padding(start = if (badge == null) Spacing.roomy else Spacing.tight)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = Spacing.roomy),
                content = actions
            )
        }
    )
}

/**
 * Опознавательный значок в кружке.
 *
 * Своя разметка вместо готового составного: у Material 3 нет отдельного
 * элемента для такого значка — гайдлайн описывает его содержимым
 * `navigationIcon` и оставляет вид на усмотрение приложения.
 */
@Composable
private fun TopBarBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .padding(start = Spacing.roomy)
            .size(Sizes.headerIcon)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
