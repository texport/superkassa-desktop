package kz.mybrain.superkassa.presentation.cabinet.register.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.model.documents.TechnicalState
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.common.section.CollapsibleCard
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Карточка технического состояния рисуется во всех своих случаях.
 *
 * Читаемость карточки проверяется глазами по картинке, но сами случаи
 * обязаны собираться без падений и отличаться друг от друга: владелец
 * жаловался ровно на то, что карточка выглядит одинаково непонятно
 * и при согласии источников, и при расхождении.
 *
 * Картинки кадров остаются в `/tmp/tech-state-*.png`: по ним видно, что
 * плашки не уехали за край, а ответ стоит над показаниями.
 */
class TechnicalStateRenderTest {

    private val texts = textsOf(Language.Ru).cabinet

    private fun node(state: String) = CoreScene.kkm(state = state, id = "k-1").copy(ofdSystemId = "5000021")

    private fun register(status: String) = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = status)

    private fun snapshot(
        active: Boolean? = true,
        shift: String? = "CLOSED",
        number: Int? = 42,
        contact: String? = "2026-09-20T13:47:00Z"
    ) = RegisterState(
        cashRegisterId = "r-1",
        businessStatus = "REGISTERED",
        technicalState = TechnicalState(
            found = true,
            active = active,
            shiftStatus = shift,
            shiftNumber = number,
            lastContactAt = contact
        )
    )

    @Composable
    private fun Card(kkm: KkmResponse?, entry: CabinetRegister, state: RegisterState?) {
        val claims = stateClaims(kkm, entry, state?.technicalState)
        val answers = stateAnswers(claims)
        val work = answers.first { it.question == StateQuestion.Usable }
        CollapsibleCard(
            title = texts.technicalState,
            expanded = true,
            onToggle = {},
            trailing = { TechnicalHeader(texts, work, disagreeing(claims).isNotEmpty()) }
        ) {
            RegisterTechnical(state, texts, answers)
        }
    }

    private fun draw(name: String, width: Int = WIDTH, content: @Composable () -> Unit): ByteArray {
        val frame = RenderProbe(width = width, height = HEIGHT, content = content).use { it.frame() }
        File("/tmp/tech-state-$name.png").writeBytes(frame)
        return frame
    }

    @Test
    fun `карточка рисуется в каждом своём состоянии и все они разные`() {
        val frames = mapOf(
            "agreed" to draw("agreed") {
                Card(node("ACTIVE"), register("REGISTERED"), snapshot())
            },
            "record" to draw("record") {
                Card(node("ACTIVE"), register("DEREGISTERED"), snapshot())
            },
            "shift" to draw("shift") {
                Card(node("ACTIVE"), register("REGISTERED"), snapshot(shift = "OPEN"))
            },
            "no-node" to draw("no-node") {
                Card(null, register("REGISTERED"), snapshot())
            },
            "no-bfd" to draw("no-bfd") {
                Card(node("ACTIVE"), register("REGISTERED"), null)
            },
            "first-day" to draw("first-day") {
                Card(node("ACTIVE"), register("REGISTERED"), snapshot(number = null, contact = null))
            }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertEquals(frames.size, frames.values.map { it.toList() }.distinct().size, "случаи неотличимы друг от друга")
    }

    /**
     * Подсказка у заголовка открывается нажатием.
     *
     * Объяснение раздела — кто такие узел, кабинет и БФД и зачем их
     * сверять — на экране не лежит: владелец читает его один раз,
     * а место абзац занимал бы всегда.
     */
    @Test
    fun `объяснение раздела открывается со значка у заголовка`() {
        RenderProbe(width = WIDTH, height = HEIGHT, content = {
            Card(node("ACTIVE"), register("REGISTERED"), snapshot())
        }).use { probe ->
            val closed = probe.frame()
            probe.click(Offset(INFO_X, INFO_Y))
            val opened = probe.frame()
            File("/tmp/tech-state-tip.png").writeBytes(opened)
            assertTrue(!opened.contentEquals(closed), "подсказка не открылась")
        }
    }

    /**
     * В узкой колонке плашки переносятся, а не обрезаются.
     *
     * Показание стало целой фразой вместо «Да», и три таких в строку
     * помещаются только на широком месте: перенос обязан работать,
     * иначе владелец увидит обрубленное «касса снята с уч…».
     */
    @Test
    fun `в узкой колонке показания переносятся`() {
        val narrow = draw("narrow", width = NARROW) {
            Card(node("ACTIVE"), register("DEREGISTERED"), snapshot())
        }
        val wide = draw("record") {
            Card(node("ACTIVE"), register("DEREGISTERED"), snapshot())
        }

        assertTrue(narrow.isNotEmpty())
        assertTrue(!narrow.contentEquals(wide), "узкая колонка раскладывается иначе")
    }

    private companion object {
        /** Ширина колонки карточки кассы в рабочем месте владельца. */
        const val WIDTH = 700

        /** Та же карточка в самой узкой колонке, какая бывает у владельца. */
        const val NARROW = 420
        const val HEIGHT = 560

        /** Значок объяснения в строке заголовка — по нему и нажимаем. */
        const val INFO_X = 481f
        const val INFO_Y = 40f
    }
}
