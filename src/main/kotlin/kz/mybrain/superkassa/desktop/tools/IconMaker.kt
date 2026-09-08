package kz.mybrain.superkassa.desktop.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skiko.MainUIDispatcher
import java.io.File

/**
 * Рисует значок приложения из готовой иконки Material 3.
 *
 * Значок собирается из того же набора, что и значки в интерфейсе:
 * рисовать его отдельно значит завести вторую, расходящуюся картинку.
 * Запускается руками при смене значка, результат кладётся в репозиторий —
 * сборке приложения рисование не нужно.
 */
fun main() {
    val size = 1024
    val scene = ImageComposeScene(width = size, height = size, density = Density(1f)) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color(0xFF4B4FBF), RoundedCornerShape(size * CORNER))
                .padding((size * PADDING).dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.kkm,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    val image = scene.render()
    val png = image.encodeToData(EncodedImageFormat.PNG) ?: error("значок не нарисовался")
    File("icon.png").writeBytes(png.bytes)
    scene.close()
    runBlocking(MainUIDispatcher) { }
}

/** Скругление угла значка: доля от стороны, как в macOS. */
private const val CORNER = 0.22f

/** Поле вокруг иконки внутри плитки. */
private const val PADDING = 0.22f
