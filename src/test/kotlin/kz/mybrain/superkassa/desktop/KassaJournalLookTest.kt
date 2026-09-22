package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.ReceiptPreview
import kz.mybrain.superkassa.desktop.ui.history.HistoryScreen
import kz.mybrain.superkassa.desktop.ui.history.JournalHeader
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Журнал кассы и предпросмотр печатной формы.
 *
 * Снимки — `/tmp/kassa-journal-*.png` и `/tmp/kassa-preview-*.png`.
 * Смотреть надо на столбцы: не разъехались ли подписи, стоит ли сумма
 * вправо одной шириной и не обещает ли прочерк в столбце суммы, что
 * по документу ничего не прошло.
 */
class KassaJournalLookTest {

    private fun document(
        no: Long,
        type: String = "SALE",
        tiyn: Long? = 120_000,
        status: String = "SENT",
        error: Int? = null
    ) = Document(
        id = "doc-$no",
        docNo = no,
        docType = type,
        ofdStatus = status,
        ofdErrorCode = error,
        fiscalSign = "38%06d".format(no),
        totalAmount = tiyn,
        createdAt = System.currentTimeMillis() - no * 120_000,
        shiftNo = 7
    )

    @Test
    fun `журнал собирается пустым, полным и с отказным документом`() {
        val hundred = (1L..100L).map { document(it, tiyn = it * 15_000) }
        val mixed = listOf(
            document(1),
            document(2, type = "Z_REPORT", tiyn = 0),
            document(3, type = "SHIFT_OPEN", tiyn = 0),
            document(4, status = "FAILED", error = 4),
            document(5, status = "PENDING")
        )

        // Сеанс создаётся до сцены, а не в её содержимом: вызов внутри
        // содержимого повторяется на каждой перерисовке, и журнал
        // с сотней строк оставался пустым — состояние доставалось
        // каждый раз новому сеансу.
        val emptyDay = KassaScene.session("journal-empty", shift = KassaScene.openShift())
        val fullDay = KassaScene.session("journal-hundred", shift = KassaScene.openShift(), journal = hundred)
        val mixedDay = KassaScene.session("journal-mixed", shift = KassaScene.openShift(), journal = mixed)
        // Узел не отвечает на список документов: прежде журнал выдавал это
        // за срок без документов, и владелец уходил с экрана уверенный,
        // что за день ничего не пробито.
        val silent = KassaScene.session("journal-silent", available = false, journalAnswered = false)

        val frames = mapOf(
            "empty" to KassaScene.shot("journal-empty") { HistoryScreen(emptyDay) },
            "hundred" to KassaScene.shot("journal-hundred") { HistoryScreen(fullDay) },
            "mixed" to KassaScene.shot("journal-mixed") { HistoryScreen(mixedDay) },
            "node-silent" to KassaScene.shot("journal-node-silent") { HistoryScreen(silent) }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        // Молчащий узел входит в набор наравне с остальными: «документов
        // за срок нет» — утверждение о кассе, и говорить его можно только
        // вслед за ответом узла.
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "состояния журнала неотличимы друг от друга"
        )
    }

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
            // Значок «уже» стоит в строке действий окна: узкая лента — тот
            // же чек, и обрезаться на ней ничего не должно.
            repeat(NARROWING) { probe.click(Offset(ZOOM_OUT_X, ZOOM_OUT_Y)) }
            val narrow = probe.frame()
            File("/tmp/kassa-preview-tape-narrow.png").writeBytes(narrow)

            assertTrue(!narrow.contentEquals(wide), "лента не сузилась")
        }
    }

    private companion object {
        val TEXTS = journalTexts(Language.Ru).history

        /** Лента чека: длинная и узкая, как её рисует узел. */
        const val TAPE_WIDTH = 384
        const val TAPE_HEIGHT = 1200
        const val SETTLE = 40

        /** Значок «уже» в строке действий окна просмотра. */
        const val ZOOM_OUT_X = 973f
        const val ZOOM_OUT_Y = 69f
        const val NARROWING = 3
    }
}
