package kz.mybrain.superkassa.desktop.ui

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.AvailableUpdate
import kz.mybrain.superkassa.desktop.app.openInBrowser
import kz.mybrain.superkassa.desktop.ui.components.ConfirmActionDialog
import kz.mybrain.superkassa.desktop.ui.strings.UpdateTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Предложение скачать новую версию.
 *
 * Тот же вопрос, что и перед закрытием смены: что произойдёт и что
 * делать. «Скачать» открывает установщик под эту систему в браузере;
 * когда такого файла в выпуске нет — страницу выпуска. «Позже» ничего
 * не забывает: угол рельса остаётся подсвеченным до установки.
 */
@Composable
internal fun UpdateDialog(update: AvailableUpdate, texts: UpdateTexts, onClose: () -> Unit) {
    ConfirmActionDialog(
        icon = AppIcons.update,
        what = "${texts.available} ${update.version}",
        explain = texts.availableHint,
        action = texts.download,
        cancel = texts.later,
        onCancel = onClose,
        onConfirm = {
            openInBrowser(update.download ?: update.page)
            onClose()
        }
    )
}
