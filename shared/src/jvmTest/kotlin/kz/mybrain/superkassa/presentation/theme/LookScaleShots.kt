package kz.mybrain.superkassa.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.kassa.sale.SaleContent
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.settings.SettingsScene
import kz.mybrain.superkassa.presentation.settings.SettingsScreen
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.theme.color.Accent
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Крайние ступени размера в низком окне.
 *
 * Ступень задаётся множителем ко всей шкале, и предел у неё не
 * вычисляется — он упирается в разметку: рельс разделов меряется
 * по подписи и на крупной ступени отъедает ширину у работы, а карточки
 * настроек растут вниз. Окно 1000×700 — самое тесное из тех, за которыми
 * работают: ноутбук кассира и экран прилавка. Кадры кладутся в `/tmp`,
 * и смотрит их человек: обрезанный рельс проверка числом не поймает.
 */
class LookScaleShots {

    @Test
    fun `крайние ступени размера собираются в низком окне`() {
        val frames = TextScale.entries.associateWith { scale ->
            shot("settings-${scale.code}", Look(textScale = scale)) { desk -> Rail(desk, Section.Settings) }
        }
        frames.forEach { (scale, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр настроек при $scale") }
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "ступени размера не изменили экран настроек"
        )
    }

    @Test
    fun `экран продажи собирается на крайних ступенях`() {
        TextScale.entries.forEach { scale ->
            val frame = shot("sale-${scale.code}", Look(textScale = scale)) { desk ->
                Rail(desk, Section.Sale)
            }
            assertTrue(frame.isNotEmpty(), "пустой кадр продажи при $scale")
        }
    }

    /** Тона в тёмной теме: кружки выбора и карточки одним взглядом. */
    @Test
    fun `тона видны рядом в обеих темах`() {
        listOf(Appearance.Light, Appearance.Dark).forEach { appearance ->
            listOf(Accent.Indigo, Accent.Emerald, Accent.Lilac).forEach { accent ->
                val frame = shot(
                    name = "accent-${accent.code}-${appearance.code}",
                    look = Look(accent = accent),
                    appearance = appearance
                ) { desk -> Rail(desk, Section.Settings) }
                assertTrue(frame.isNotEmpty(), "пустой кадр тона $accent при $appearance")
            }
        }
    }

    /** Рельс и экран рядом — так, как это стоит в окне кассы. */
    @Composable
    private fun Rail(desk: KassaDesk, section: Section) {
        Row(modifier = Modifier.fillMaxSize()) {
            SectionRail(
                sections = Section.entries,
                current = section,
                collapsed = false,
                onToggle = {},
                footer = { Box(Modifier) }
            ) {}
            when (section) {
                Section.Sale -> SaleContent(SaleUiState(kkm = CoreScene.kkm(), signedIn = true, shiftOpen = true))
                else -> SettingsScreen(SettingsScene.board(desk))
            }
        }
    }

    private fun shot(
        name: String,
        look: Look,
        appearance: Appearance = Appearance.Light,
        content: @Composable (KassaDesk) -> Unit
    ): ByteArray {
        val desk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))
        val frame = RenderProbe(WIDE, LOW, appearance, look) { content(desk) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        File("/tmp/look-$name.png").writeBytes(frame)
        return frame
    }

    private companion object {
        /** Самое тесное окно, за которым работают: ноутбук и экран прилавка. */
        const val WIDE = 1000
        const val LOW = 700
        const val SETTLE = 6
    }
}
