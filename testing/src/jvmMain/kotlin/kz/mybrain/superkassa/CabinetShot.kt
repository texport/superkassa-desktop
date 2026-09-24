package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import java.io.File

/**
 * Снимок экрана в файл.
 *
 * Кадров нужно много: списки и карточки кабинета приходят отложенным
 * эффектом, и на первом кадре экран ещё пуст. Сцена отдаёт кадры только
 * по запросу, поэтому очередь обращений докручивается вручную.
 */
fun shot(name: String, width: Int = 1180, height: Int = 820, content: @Composable () -> Unit): ByteArray {
    RenderProbe(width = width, height = height, content = content).use { probe ->
        var frame = probe.frame()
        repeat(60) { frame = probe.frame() }
        File("/tmp/cabinet-$name.png").writeBytes(frame)
        return frame
    }
}
