package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Окно заполнения: одно на все формы кабинета.
 *
 * Формы заведения стояли карточками в рабочей области и отжимали списки
 * вниз. Заводят точку и кассу раз в жизни, а список смотрят каждый день —
 * значит место формы в окне, а списка на экране.
 *
 * Незаполненное перечислено под кнопкой словами, а не подписями
 * «Обязательно» под каждым полем: подпись висит всегда и читается как
 * часть поля, а перечень появляется тогда, когда он нужен, — и называет
 * ровно то, чего не хватает.
 *
 * @param missing чего не хватает; пустой список открывает кнопку.
 */
@Composable
fun FormDialog(
    title: String,
    icon: ImageVector,
    action: String,
    close: String,
    busy: Boolean,
    missing: List<String>,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(Sizes.formDialog),
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug),
                content = content
            )
        },
        confirmButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Button(enabled = !busy && missing.isEmpty(), onClick = onAction) { Text(action) }
                MissingLine(missing)
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(close) } }
    )
}

/** Чего не хватает — одной строкой под кнопкой. */
@Composable
private fun MissingLine(missing: List<String>) {
    if (missing.isEmpty()) return
    Text(
        text = missing.joinToString(", "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
