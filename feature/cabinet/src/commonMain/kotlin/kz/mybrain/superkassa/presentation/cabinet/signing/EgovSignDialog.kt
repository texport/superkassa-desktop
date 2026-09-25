package kz.mybrain.superkassa.presentation.cabinet.signing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.designsystem.dialog.DialogBody
import kz.mybrain.superkassa.designsystem.dialog.DialogTitle
import kz.mybrain.superkassa.designsystem.dialog.formDialogWidth
import kz.mybrain.superkassa.designsystem.image.encodedImage
import kz.mybrain.superkassa.designsystem.keyboard.CloseOnEscape
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts

/**
 * Подпись в eGov mobile: QR для телефона и кнопка приложения на этом устройстве.
 *
 * Диалог Material 3: одно главное действие — открыть eGov mobile здесь;
 * на планшете кассы eGov mobile обычно нет, и владелец читает QR своим
 * телефоном — поэтому QR стоит в теле окна крупно. Окно само не закрывается
 * касанием мимо: случайное касание обрывало бы подпись, которую владелец
 * как раз подтверждает на телефоне. Отмена — кнопкой, «назад» или Escape.
 *
 * Подпись приходит своим путём, и окно закрывается само, как только она есть.
 */
@Composable
internal fun EgovSignDialog(request: SignRequest.EgovMobile, eds: EdsTexts, onCancel: () -> Unit) {
    val links = LocalUriHandler.current
    CloseOnEscape(onCancel)
    val qr = remember(request) { encodedImage(request.qr) }
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
        modifier = Modifier.formDialogWidth(),
        icon = { Icon(AppIcons.signOnPhone, contentDescription = null) },
        title = { DialogTitle(eds.egovTitle) },
        text = { EgovSignBody(qr, eds) },
        confirmButton = { Button(onClick = { runCatching { links.openUri(request.launch) } }) { Text(eds.egovOpen) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(eds.cancel) } }
    )
}

/** Что сделать и QR для телефона. */
@Composable
private fun EgovSignBody(qr: ImageBitmap?, eds: EdsTexts) = DialogBody {
    Text(eds.egovScan, style = MaterialTheme.typography.bodyMedium)
    qr?.let {
        Image(
            bitmap = it,
            contentDescription = eds.egovQr,
            // Клетки QR — чёткими квадратами: сглаженные края телефон читает хуже.
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(Sizes.signQr).align(Alignment.CenterHorizontally)
        )
    }
}
