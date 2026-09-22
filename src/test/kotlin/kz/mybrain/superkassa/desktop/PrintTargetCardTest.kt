package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.settings.PrintTargetCard
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
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
        val session = KassaScene.session("print-target")
        val without = shot("print-target-none", session, printers = emptyList())
        val with = shot("print-target-some", session, printers = listOf("Чековый у кассы"))
        assertTrue(!without.contentEquals(with), "пустая машина выглядит так же, как машина с принтером")
    }

    private fun shot(name: String, session: kz.mybrain.superkassa.desktop.app.Session, printers: List<String>) =
        RenderProbe(width = WIDTH, height = HEIGHT) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.screen)) { PrintTargetCard(session, printers) }
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
