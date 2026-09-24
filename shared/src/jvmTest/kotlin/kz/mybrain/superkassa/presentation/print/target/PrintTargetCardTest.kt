package kz.mybrain.superkassa.presentation.print.target

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.shot
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Карточка принтера, когда принтера нет.
 *
 * Рабочее место за прилавком ставят раньше, чем подключают чековый
 * принтер. Карточка предлагала «системный по умолчанию» и молчала,
 * а кассир узнавал правду отказом печати на первом же чеке.
 */
class PrintTargetCardTest {

    @Test
    fun `без принтеров карточка говорит об этом словами`() {
        val without = shot("print-target-none", printers = emptyList())
        val with = shot("print-target-some", printers = listOf("Чековый у кассы"))
        assertTrue(!without.contentEquals(with), "пустая машина выглядит так же, как машина с принтером")
    }

    private fun shot(name: String, printers: List<String>) =
        RenderProbe(width = WIDTH, height = HEIGHT) {
            val target = PrintTargetUiState(kkmId = "kkm-1", printers = printers, printersRead = true)
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.screen)) { PrintTargetCard(target, object : PrintTargetActions {}) }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame().also { File("/tmp/kassa-$name.png").writeBytes(it) }
        }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 420
        const val SETTLE = 12
    }
}
