package kz.mybrain.superkassa

import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.Typeface
import kz.mybrain.superkassa.presentation.theme.choice.LookUiState
import kz.mybrain.superkassa.presentation.theme.color.Accent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Язык, тон, шрифт, размер и свёрнутые части переживают перезапуск.
 *
 * Кассир выбирает их один раз под свой монитор и своё зрение; касса,
 * забывшая выбор за ночь, каждое утро встречала бы его мелким индиго.
 */
class LookPreferencesTest {

    @Test
    fun `выбор оформления запоминается и читается тем же`() {
        val home = freshHome()
        val file = File(home, "kkm")

        assertEquals(Look(), shown(file).look, "без выбора — касса как была")

        WorkplaceLook(Preferences(file)).let { look ->
            look.choose(
                look.state.value.copy(
                    language = Language.En.code,
                    accent = Accent.Teal.code,
                    typeface = Typeface.Serif.code,
                    textScale = TextScale.Large.code,
                    railCollapsed = true
                )
            )
        }

        val saved = shown(file)
        assertEquals(Look(accent = Accent.Teal, typeface = Typeface.Serif, textScale = TextScale.Large), saved.look)
        assertEquals(Language.En, saved.language)
        assertTrue(saved.railCollapsed, "свёрнутый рельс развернулся после перезапуска")
        home.deleteRecursively()
    }

    @Test
    fun `испорченный файл настройки не ломает запуск`() {
        val home = freshHome()
        File(home, "accent").writeText("chartreuse")
        File(home, "textscale").writeText("")

        assertEquals(Look(), shown(File(home, "kkm")).look)
        home.deleteRecursively()
    }

    /** Вид окна, каким его прочтёт следующий запуск. */
    private fun shown(file: File) = LookUiState.of(WorkplaceLook(Preferences(file)).state.value)

    private fun freshHome(): File = File.createTempFile("look", "").also {
        it.delete()
        it.mkdirs()
    }
}
