package kz.mybrain.superkassa.designsystem.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.designsystem.keyboard.CloseOnEscape
import kz.mybrain.superkassa.designsystem.theme.size.Sizes

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
        // Не шире формы и не шире окна: на узком окне форма ужимается
        // вместе с ним, а не уходит краем за него.
        modifier = Modifier.widthIn(max = Sizes.formDialog).fillMaxWidth(),
        icon = { Icon(icon, contentDescription = null) },
        title = { DialogTitle(title) },
        text = { DialogBody(content = content) },
        confirmButton = {
            Button(enabled = !busy && missing.isEmpty(), onClick = onAction) { Text(action) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(close) } }
    )
}
