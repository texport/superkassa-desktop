package kz.mybrain.superkassa.presentation.cabinet.applications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.viewOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Виды заявления читаются целиком в карточке кассы.
 *
 * Карточка кассы в окне 1180×820 шириной около четырёхсот двадцати точек,
 * а три сегмента равной ширины по самой длинной подписи в неё не входили:
 * ряд ужимался, и владелец читал «Поставить на учё» и «Перерегистриро».
 */
class ApplicationKindFitTest {

    @Test
    fun `виды заявления не обрезаются в карточке кассы`() {
        Language.entries.forEach { language ->
            listOf(TextScale.Normal, TextScale.Larger).forEach { scale -> check(language, scale) }
        }
    }

    private fun check(language: Language, scale: TextScale) {
        val texts = textsOf(language).cabinet
        val stage = CabinetStage { StubReply("{}") }
        val register = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "REGISTERED")
        RenderProbe(WIDTH, HEIGHT, look = Look(textScale = scale), language = language) {
            stage.Window {
                Column(Modifier.width(CARD.dp)) {
                    RegistrationActionsBlock(stage.cabinet.cabinet, language, texts, viewOf(register)) {}
                }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val kinds = ActionKind.entries.map { it.title(texts) }.toSet()
            val cut = probe.nodes { nodes ->
                nodes.filter { node ->
                    node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } in kinds &&
                        node.cut()
                }.map { it.config[SemanticsProperties.Text].joinToString("") { text -> text.text } }
            }
            assertTrue(cut.isEmpty(), "$language $scale: обрезаны виды заявления $cut")
        }
    }

    /**
     * Подпись не влезла в свою строку: её полная ширина больше отведённой.
     * Признак переполнения самой раскладки тут не годится — раскладку
     * семантика отдаёт по ширине родителя, а не по ширине подписи.
     */
    private fun SemanticsNode.cut(): Boolean {
        val layout = layout() ?: return false
        return layout.lineCount == 1 && layout.multiParagraph.intrinsics.maxIntrinsicWidth > size.width + 1
    }

    private fun SemanticsNode.layout(): TextLayoutResult? {
        val results = mutableListOf<TextLayoutResult>()
        config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.firstOrNull()
    }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 700
        const val CARD = 420
        const val SETTLE = 24
    }
}
