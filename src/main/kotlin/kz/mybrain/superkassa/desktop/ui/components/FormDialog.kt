package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
 * Незаполненное видно по самой форме, и подписи под кнопкой нет:
 * перечень пустых полей повторял их названия второй раз и читался
 * как отказ, хотя владелец ещё не нажимал. Кнопка просто погашена,
 * пока форма не заполнена.
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
    CloseOnEscape { if (!busy) onDismiss() }
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
            Button(enabled = !busy && missing.isEmpty(), onClick = onAction) { Text(action) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(close) } }
    )
}
