package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionRail
import kz.mybrain.superkassa.desktop.ui.sale.SaleScreen
import kz.mybrain.superkassa.desktop.ui.settings.SettingsScreen
import kz.mybrain.superkassa.desktop.ui.theme.Accent
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Look
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
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
            shot("settings-${scale.code}", Look(textScale = scale)) { session -> Rail(session, Section.Settings) }
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
            val frame = shot("sale-${scale.code}", Look(textScale = scale)) { session ->
                Rail(session, Section.Sale)
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
                ) { session -> Rail(session, Section.Settings) }
                assertTrue(frame.isNotEmpty(), "пустой кадр тона $accent при $appearance")
            }
        }
    }

    /** Рельс и экран рядом — так, как это стоит в окне кассы. */
    @Composable
    private fun Rail(session: Session, section: Section) {
        Row(modifier = Modifier.fillMaxSize()) {
            SectionRail(
                sections = Section.entries,
                current = section,
                collapsed = false,
                onToggle = {},
                footer = { Box(Modifier) }
            ) {}
            when (section) {
                Section.Sale -> SaleScreen(session)
                else -> SettingsScreen(session)
            }
        }
    }

    private fun shot(
        name: String,
        look: Look,
        appearance: Appearance = Appearance.Light,
        content: @Composable (Session) -> Unit
    ): ByteArray {
        val session = KassaScene.session("look-$name", shift = KassaScene.openShift())
        val frame = RenderProbe(WIDE, LOW, appearance, look) { content(session) }.use { probe ->
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
