package kz.mybrain.superkassa.presentation.shell.starting

import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kassa.model.StartProblem
import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import kz.mybrain.superkassa.presentation.words.shell.of
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Касса не открылась — кассир видит экран, а не падение.
 *
 * На экране причина, то, что делать, сведения для обслуживания и одно
 * действие — закрыть. На каждом из трёх языков и для каждой причины
 * слова свои: одинаковые слова на разные причины отправляли бы кассира
 * делать не то.
 */
class StartRefusedScreenTest {

    @Test
    fun `причина, что делать и закрытие — на каждом языке`() {
        Language.entries.forEach { language ->
            val texts = textsOf(language).shell
            val problem = StartProblem(StartRefusal.NodeRunning, "Node answers at 127.0.0.1:8080")
            RenderProbe(WIDTH, HEIGHT, language = language) { StartRefusedScreen(problem) {} }.use { probe ->
                probe.frame()
                File("/tmp/start-refused-${language.code}.png").writeBytes(probe.frame())
                val said = probe.nodes().map { it.text }
                listOf(texts.title, texts.nodeRunning.reason, texts.nodeRunning.action).forEach { words ->
                    assertTrue(words in said, "$language: на экране нет «$words»")
                }
                assertTrue(said.any { problem.detail in it }, "$language: сведений для обслуживания нет")
                val close = probe.nodes().firstOrNull { it.text == texts.close && it.enabled }
                assertNotNull(close, "$language: не закрыть")
            }
        }
    }

    @Test
    fun `у каждой причины свои слова`() {
        Language.entries.forEach { language ->
            val texts = textsOf(language).shell
            val words = StartRefusal.entries.map(texts::of)
            assertEquals(words.size, words.map { it.reason }.distinct().size, "$language: причины названы одинаково")
            assertEquals(words.size, words.map { it.action }.distinct().size, "$language: действия названы одинаково")
            assertTrue(words.all { it.reason.isNotBlank() && it.action.isNotBlank() }, "$language: пустые слова")
        }
    }

    private companion object {
        const val WIDTH = 1000
        const val HEIGHT = 700
    }
}
