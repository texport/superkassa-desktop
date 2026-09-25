package kz.mybrain.superkassa.designsystem.list

import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow

/**
 * Строка списка разделов: значок, название и сводка под ним.
 *
 * Строка Material 3 (`ListItem`) в панели списка «списка и подробностей».
 * Раздел, открытый в панели рядом, подсвечен контейнером `secondaryContainer`
 * — так Material 3 отмечает выбранную строку, когда подробности стоят
 * рядом со списком; на узком окне подсветки нет, выбранного там не видно.
 *
 * @param summary строка под названием: что в разделе или что в нём выбрано.
 */
@Composable
fun SectionListItem(
    icon: ImageVector,
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val colors = if (selected) {
        ListItemDefaults.colors(
            containerColor = scheme.secondaryContainer,
            headlineColor = scheme.onSecondaryContainer,
            leadingIconColor = scheme.onSecondaryContainer,
            supportingColor = scheme.onSecondaryContainer
        )
    } else {
        ListItemDefaults.colors(containerColor = Color.Transparent)
    }
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = colors,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
    )
}
