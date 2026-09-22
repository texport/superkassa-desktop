package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Последний вопрос перед необратимым.
 *
 * Снятие кассы с учёта и удаление кассира спрашивают одинаково: значок
 * предупреждения, что именно исчезнет, и подтверждение ролью ошибки —
 * красная кнопка отличает необратимое от обычного действия с одного
 * взгляда. Раньше это окно стояло в двух местах слово в слово, и правка
 * в одном расходилась с другим.
 *
 * @param what что произойдёт — заголовок с названием того, что исчезнет.
 * @param explain последствие словами: его читают один раз и решают.
 */
@Composable
fun ConfirmDangerDialog(
    what: String,
    explain: String,
    action: String,
    cancel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    CloseOnEscape(onCancel)
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = AppIcons.warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(what) },
        text = { Text(explain, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(action) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(cancel) } }
    )
}

/**
 * Вопрос перед необратимым, которое не разрушает.
 *
 * Z-отчёт смену не удаляет, но и не отменяется: спрашивается так же,
 * как внесение денег, — что произойдёт и что станет с итогами. Красной
 * кнопки здесь нет: красный отличает уничтожение от обычной работы,
 * а закрытие смены — обычная работа конца дня.
 *
 * @param what что произойдёт — заголовок с тем, чего касается вопрос.
 * @param explain последствие числами: их читают один раз и решают.
 */
@Composable
fun ConfirmActionDialog(
    icon: ImageVector,
    what: String,
    explain: String,
    action: String,
    cancel: String,
    busy: Boolean = false,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(what) },
        text = { Text(explain, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { Button(enabled = !busy, onClick = onConfirm) { Text(action) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onCancel) { Text(cancel) } }
    )
}
