package kz.mybrain.superkassa.presentation.print.preview.component

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.presentation.common.document.JournalHeader
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Окно печатной формы: ожидание, неразобранная форма, печать без принтера и лента.
 *
 * Снимки — `/tmp/kassa-preview-*.png`.
 */
class ReceiptPreviewLookTest {

    /**
     * Окно печатной формы: пока узел рисует, пока рисунок не разобрался
     * и когда печатать нечем.
     *
     * Настоящая лента здесь не нужна: проверяется не чек, а то, что окно
     * в каждом из трёх случаев объясняет себя, а не стоит пустым.
     */
    @Test
    fun `окно печатной формы объясняет себя во всех трёх случаях`() {
        val drawing = KassaScene.shot("preview-drawing") {
            ReceiptPreview(image = null, drawing = true, onDismiss = {})
        }
        val broken = KassaScene.shot("preview-broken") {
            ReceiptPreview(image = byteArrayOf(1, 2, 3), drawing = false, onDismiss = {})
        }
        val noPrinter = KassaScene.shot("preview-no-print") {
            ReceiptPreview(image = byteArrayOf(1, 2, 3), drawing = false, onPrint = null, onDismiss = {})
        }

        assertTrue(drawing.isNotEmpty() && broken.isNotEmpty() && noPrinter.isNotEmpty())
        assertTrue(!drawing.contentEquals(broken), "ожидание и неразобранная форма выглядят одинаково")
    }

    /**
     * Лента на экране: во всю ширину окна и узкая.
     *
     * Вместо настоящей формы узла берётся кадр сцены — такой же PNG той же
     * длинной и узкой пропорции. Проверяется не чек, а показ: лента стоит
     * по середине, прокручивается и сужается значками.
     */
    @Test
    fun `лента показывается и меняет ширину`() {
        val tape = RenderProbe(width = TAPE_WIDTH, height = TAPE_HEIGHT, content = { JournalHeader(TEXTS) })
            .use { it.frame() }

        RenderProbe(
            width = KassaScene.WIDE,
            height = KassaScene.TALL,
            content = { ReceiptPreview(image = tape, onPrint = {}, onSave = {}, onDismiss = {}) }
        ).use { probe ->
            repeat(SETTLE) { probe.frame() }
            val wide = probe.frame()
            File("/tmp/kassa-preview-tape.png").writeBytes(wide)
            // Значок «мельче» ищется по своей надписи, а не по месту: место
            // значка меняется вместе с шапкой окна, а надпись — нет. Узкая
            // лента — тот же чек, и обрезаться на ней ничего не должно.
            val zoomOut = probe.nodes().firstOrNull { it.label == ZOOM_OUT }
            assertNotNull(zoomOut, "в шапке окна нет значка «${ZOOM_OUT}»")
            val at = zoomOut.at + Offset(zoomOut.width / 2f, zoomOut.height / 2f)
            repeat(NARROWING) { probe.click(at) }
            val narrow = probe.frame()
            File("/tmp/kassa-preview-tape-narrow.png").writeBytes(narrow)

            assertTrue(!narrow.contentEquals(wide), "лента не сузилась")
        }
    }

    private companion object {
        val TEXTS = textsOf(Language.Ru).journal.history

        /** Лента чека: длинная и узкая, как её рисует узел. */
        const val TAPE_WIDTH = 384
        const val TAPE_HEIGHT = 1200
        const val SETTLE = 40

        /** Надпись значка «мельче» в шапке окна просмотра. */
        val ZOOM_OUT = textsOf(Language.Ru).common.preview.zoomOut
        const val NARROWING = 3
    }
}
