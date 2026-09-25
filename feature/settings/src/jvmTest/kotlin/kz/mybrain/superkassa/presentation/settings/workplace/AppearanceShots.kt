package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.Typeface
import kz.mybrain.superkassa.designsystem.theme.color.Accent
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.look.lookModel
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки карточки «Оформление» с выбранным тоном, шрифтом и размером.
 *
 * Смотрит человек: галочка на выбранном кружке, четырнадцать различимых
 * тонов, сегменты шрифта и размера — в светлой и в тёмной теме,
 * и они же на самой плотной и самой крупной ступени размера.
 */
class AppearanceShots {

    @Test
    fun `карточка оформления в двух темах`() {
        listOf(Appearance.Light, Appearance.Dark).forEach { appearance ->
            val look = look().apply {
                chooseAccent(Accent.Teal)
                chooseTypeface(Typeface.Serif)
                chooseTextScale(TextScale.Large)
            }
            val frame = card(appearance, look)
            val file = File("/tmp/kassa-appearance-${appearance.code}.png")
            file.writeBytes(frame)
            assertTrue(file.length() > 0, "снимок ${appearance.code} пуст")
        }
    }

    /**
     * Карточка на крайних ступенях размера.
     *
     * Ряд из пяти сегментов и ряд из четырнадцати кружков растут вместе
     * с текстом, а ширина карточки — нет: на крупной ступени ряд либо
     * помещается, либо уходит за правый край, и увидеть это можно
     * только кадром.
     */
    @Test
    fun `карточка оформления на крайних ступенях размера`() {
        listOf(TextScale.Dense, TextScale.Larger).forEach { scale ->
            val look = look().apply { chooseTextScale(scale) }
            val frame = card(Appearance.Light, look)
            val file = File("/tmp/kassa-appearance-${scale.code}.png")
            file.writeBytes(frame)
            assertTrue(file.length() > 0, "снимок ступени ${scale.code} пуст")
        }
    }

    /** Вид окна кассы, как его создаёт окно: память вида — в проверке. */
    private fun look(): LookViewModel = lookModel(CoreScene.services(FakeCore()).look)

    private fun card(appearance: Appearance, look: LookViewModel): ByteArray =
        RenderProbe(width = WIDTH, height = HEIGHT, appearance = appearance, look = look.state.value.look) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.fieldGap)) { AppearanceCard(look) }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }

    private companion object {
        const val WIDTH = 1000
        const val HEIGHT = 560
        const val SETTLE = 12
    }
}
