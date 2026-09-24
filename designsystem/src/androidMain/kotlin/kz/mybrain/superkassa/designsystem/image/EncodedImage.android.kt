package kz.mybrain.superkassa.designsystem.image

import android.graphics.BitmapFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun encodedImage(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

actual fun Modifier.zoomByWheel(onZoom: (Float) -> Unit): Modifier = this
