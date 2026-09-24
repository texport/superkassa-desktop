package kz.mybrain.superkassa.presentation.update.check

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.dialog.ConfirmActionDialog
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.domain.update.model.AvailableUpdate
import kz.mybrain.superkassa.strings.api.update.UpdateTexts

/**
 * Предложение скачать новую версию.
 *
 * Тот же вопрос, что и перед закрытием смены: что произойдёт и что
 * делать. «Скачать» открывает установщик под эту систему в браузере;
 * когда такого файла в выпуске нет — страницу выпуска. «Позже» ничего
 * не забывает: угол рельса остаётся подсвеченным до установки.
 */
@Composable
fun UpdateDialog(
    update: AvailableUpdate,
    texts: UpdateTexts,
    onInstall: (AvailableUpdate) -> Unit,
    onClose: () -> Unit
) {
    ConfirmActionDialog(
        icon = AppIcons.update,
        what = "${texts.available} ${update.version}",
        explain = texts.availableHint,
        action = texts.download,
        cancel = texts.later,
        onCancel = onClose,
        onConfirm = {
            onInstall(update)
            onClose()
        }
    )
}
