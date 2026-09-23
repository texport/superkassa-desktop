package kz.mybrain.superkassa

import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.presentation.session.WorkplaceSettings
import kz.mybrain.superkassa.presentation.theme.Accent
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.Typeface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Тон, шрифт и размер переживают перезапуск.
 *
 * Кассир выбирает их один раз под свой монитор и своё зрение; касса,
 * забывшая выбор за ночь, каждое утро встречала бы его мелким индиго.
 */
class LookPreferencesTest {

    @Test
    fun `выбор оформления запоминается и читается тем же`() {
        val home = File.createTempFile("look", "").also { it.delete() }
        home.mkdirs()
        val file = File(home, "kkm")

        assertEquals(Look(), WorkplaceSettings(Preferences(file)).look, "без выбора — касса как была")

        WorkplaceSettings(Preferences(file)).apply {
            chooseAccent(Accent.Teal)
            chooseTypeface(Typeface.Serif)
            chooseTextScale(TextScale.Large)
        }

        assertEquals(
            Look(accent = Accent.Teal, typeface = Typeface.Serif, textScale = TextScale.Large),
            WorkplaceSettings(Preferences(file)).look
        )
        home.deleteRecursively()
    }

    @Test
    fun `испорченный файл настройки не ломает запуск`() {
        val home = File.createTempFile("look", "").also { it.delete() }
        home.mkdirs()
        File(home, "accent").writeText("chartreuse")
        File(home, "textscale").writeText("")

        assertEquals(Look(), WorkplaceSettings(Preferences(File(home, "kkm"))).look)
        home.deleteRecursively()
    }
}
