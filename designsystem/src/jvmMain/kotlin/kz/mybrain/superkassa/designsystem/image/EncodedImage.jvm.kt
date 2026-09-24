package kz.mybrain.superkassa.designsystem.image

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import org.jetbrains.skia.Image

actual fun encodedImage(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.zoomByWheel(onZoom: (Float) -> Unit): Modifier =
    onPointerEvent(PointerEventType.Scroll, PointerEventPass.Initial) { event ->
        if (!event.keyboardModifiers.isCtrlPressed) return@onPointerEvent
        val change = event.changes.first()
        onZoom(-change.scrollDelta.y)
        change.consume()
    }
