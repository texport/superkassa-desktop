package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.kassa.inlineMain
import java.io.File
import kotlin.test.assertTrue

/**
 * Кадры по замечаниям владельца — в окне ноутбука и на мониторе.
 *
 * Экраны разных областей снимаются одним порядком: те же окна, те же
 * кадры до снимка, те же имена `/tmp/owner-<экран>-<окно>.png`, — чтобы
 * владелец листал их подряд.
 */
object OwnerShots {

    /** Ноутбук 1280×800 и монитор 1920×1080. */
    private val SIZES = listOf(1280 to 800, 1920 to 1080)

    private const val SETTLE = 30

    /** Снимает [shoot] в каждом окне. */
    fun each(shoot: (Int, Int) -> Unit) = SIZES.forEach { (width, height) -> shoot(width, height) }

    /** Кадр [content] в окне [width]×[height] под именем [name]; пустой кадр — провал проверки. */
    fun save(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val frame = inlineMain {
            RenderProbe(width, height, content = content).use { probe ->
                repeat(SETTLE) { probe.frame() }
                probe.frame()
            }
        }
        val file = File("/tmp/owner-$name-${width}x$height.png")
        file.writeBytes(frame)
        assertTrue(file.length() > 0, "кадр $name пуст")
    }
}
