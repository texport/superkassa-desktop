package kz.mybrain.superkassa.presentation.cabinet.signing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kz.mybrain.superkassa.designsystem.dialog.DialogBody
import kz.mybrain.superkassa.designsystem.dialog.DialogTitle
import kz.mybrain.superkassa.designsystem.dialog.formDialogWidth
import kz.mybrain.superkassa.designsystem.format.Times
import kz.mybrain.superkassa.designsystem.image.encodedImage
import kz.mybrain.superkassa.designsystem.keyboard.CloseOnEscape
import kz.mybrain.superkassa.designsystem.link.rememberAppLink
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.motion.Durations
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts
import kz.mybrain.superkassa.strings.api.fill
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Подпись в eGov mobile: QR для телефона и кнопка приложения на этом устройстве.
 *
 * Диалог Material 3: одно главное действие — открыть eGov mobile здесь;
 * на планшете кассы eGov mobile обычно нет, и владелец читает QR своим
 * телефоном — поэтому QR стоит в теле окна крупно, а под ним — сколько
 * ещё ждать. Не открылось приложение — окно говорит об этом и предлагает
 * QR, а не молчит. Окно само не закрывается касанием мимо: случайное
 * касание обрывало бы подпись, которую владелец как раз подтверждает
 * на телефоне. Отмена — кнопкой, «назад» или Escape.
 *
 * Подпись приходит своим путём, и окно закрывается само, как только она есть.
 */
@Composable
internal fun EgovSignDialog(request: SignRequest.EgovMobile, eds: EdsTexts, onCancel: () -> Unit) {
    CloseOnEscape(onCancel)
    val qr = remember(request) { encodedImage(request.qr) }
    val open = rememberAppLink(EGOV_MOBILE_APP)
    var failed by remember(request) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
        modifier = Modifier.formDialogWidth(),
        icon = { Icon(AppIcons.signOnPhone, contentDescription = null) },
        title = { DialogTitle(eds.egovTitle) },
        text = { EgovSignBody(qr, eds, failed, egovLeft(request)) },
        confirmButton = { Button(onClick = { failed = !open(request.launch) }) { Text(eds.egovOpen) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(eds.cancel) } }
    )
}

/** Что сделать, QR для телефона, сколько ждать — и почему не открылось приложение. */
@Composable
private fun EgovSignBody(qr: ImageBitmap?, eds: EdsTexts, failed: Boolean, left: Duration) = DialogBody {
    Text(eds.egovScan, style = MaterialTheme.typography.bodyMedium)
    if (failed) Text(eds.egovNotOpened, color = MaterialTheme.colorScheme.error)
    qr?.let {
        Image(
            bitmap = it,
            contentDescription = eds.egovQr,
            // Клетки QR — чёткими квадратами: сглаженные края телефон читает хуже.
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(Sizes.signQr).align(Alignment.CenterHorizontally)
        )
    }
    Text(eds.remaining.fill(Times.countdown(left)), style = MaterialTheme.typography.labelLarge)
    LinearProgressIndicator(progress = { (left / Signer.SIGN_WINDOW).toFloat() }, modifier = Modifier.fillMaxWidth())
}

/** Сколько ещё ждать подписи — от появления окна, тем же сроком, что у подписывающего. */
@Composable
private fun egovLeft(request: SignRequest.EgovMobile): Duration {
    val since = remember(request) { TimeSource.Monotonic.markNow() }
    var left by remember(request) { mutableStateOf(Signer.SIGN_WINDOW) }
    LaunchedEffect(request) {
        while (isActive) {
            left = (Signer.SIGN_WINDOW - since.elapsedNow()).coerceAtLeast(Duration.ZERO)
            delay(Durations.everySecond)
        }
    }
    return left
}

/** Пакет eGov mobile на Android: ссылка подписи адресуется ему напрямую. */
private const val EGOV_MOBILE_APP = "kz.mobile.mgov"
