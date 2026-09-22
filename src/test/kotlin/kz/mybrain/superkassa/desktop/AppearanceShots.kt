package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.settings.AppearanceCard
import kz.mybrain.superkassa.desktop.ui.theme.Accent
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import kz.mybrain.superkassa.desktop.ui.theme.Typeface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки карточки «Оформление» с выбранным тоном, шрифтом и размером.
 *
 * Смотрит человек: галочка на выбранном кружке, восемь различимых
 * тонов, сегменты шрифта и размера — в светлой и в тёмной теме.
 */
class AppearanceShots {

    @Test
    fun `карточка оформления в двух темах`() {
        listOf(Appearance.Light, Appearance.Dark).forEach { appearance ->
            val session = Look.session().apply {
                chooseAccent(Accent.Teal)
                chooseTypeface(Typeface.Serif)
                chooseTextScale(TextScale.Large)
            }
            val frame = RenderProbe(
                width = WIDTH,
                height = HEIGHT,
                appearance = appearance,
                look = session.look
            ) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(Spacing.screen)) { AppearanceCard(session) }
                }
            }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                probe.frame()
            }
            val file = File("/tmp/kassa-appearance-${appearance.code}.png")
            file.writeBytes(frame)
            assertTrue(file.length() > 0, "снимок ${appearance.code} пуст")
        }
    }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 480
        const val SETTLE = 12
    }
}
