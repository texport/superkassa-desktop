package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
