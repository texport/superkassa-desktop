package kz.mybrain.superkassa.presentation.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.common.document.JournalEntry
import kz.mybrain.superkassa.presentation.common.document.JournalHeader
import kz.mybrain.superkassa.presentation.common.document.JournalRow
import kz.mybrain.superkassa.presentation.common.document.JournalState
import kz.mybrain.superkassa.presentation.journal.documents.JournalUiState
import kz.mybrain.superkassa.presentation.print.preview.component.ReceiptPreview
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.strings.journal.journalTexts
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    ) = CoreScene.document("doc-$no", type = type, amount = tiyn, status = status).copy(
        docNo = no,
        ofdErrorCode = error,
        fiscalSign = "38%06d".format(no),
        createdAt = System.currentTimeMillis() - no * 120_000
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

        val read = JournalUiState(page = PageOutcome.page(more = false), loading = false)
        // Касса не отвечает на список документов: прежде журнал выдавал это
        // за срок без документов, и владелец уходил с экрана уверенный,
        // что за день ничего не пробито.
        val silent = JournalUiState(page = PageOutcome.unread, loading = false)

        val frames = mapOf(
            "empty" to KassaScene.shot("journal-empty") { HistoryContent(HistoryParts(read)) },
            "hundred" to KassaScene.shot("journal-hundred") {
                HistoryContent(HistoryParts(read.copy(documents = hundred)))
            },
            "mixed" to KassaScene.shot("journal-mixed") { HistoryContent(HistoryParts(read.copy(documents = mixed))) },
            "kassa-silent" to KassaScene.shot("journal-kassa-silent") { HistoryContent(HistoryParts(silent)) }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        // Молчащая касса входит в набор наравне с остальными: «документов
        // за срок нет» — утверждение о кассе, и говорить его можно только
        // вслед за её ответом.
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "состояния журнала неотличимы друг от друга"
        )
    }

    /**
     * Строка журнала не растёт от длины слова в столбце состояния.
     *
     * Плашка состояния была набрана ступенью крупнее остальных клеток,
     * и на крупном шрифте в узком окне «Доставлен» разрывалось пополам:
     * строка становилась выше соседних, а столбец сумм рвался зазорами,
     * пока половина ширины таблицы стояла свободной.
     */
    @Test
    fun `столбец состояния не рвёт слово на крупном шрифте`() {
        val long = rowHeight("Доставлен")
        val short = rowHeight(Glyphs.DASH)

        assertTrue(long > 0 && short > 0, "строка не нарисовалась")
        assertEquals(short, long, "плашка состояния перенеслась на вторую строку и подняла ряд")
    }

    /** Высота строки журнала в точках: ряд закрашен подложкой во всю ширину. */
    private fun rowHeight(state: String): Int {
        val entry = JournalEntry(
            key = "row",
            at = 0,
            moment = "22.09 22:44:02",
            typeCode = "SALE",
            type = "Продажа",
            number = "12",
            numberOrder = 12,
            amount = "1 500,00 ₸",
            amountOrder = Decimal.parse("1500.00"),
            sign = "3810000000000012",
            delivery = null,
            shiftNo = 7,
            state = JournalState(state, done = true)
        )
        val frame = RenderProbe(
            width = NARROW_WINDOW,
            height = TAPE_HEIGHT,
            look = Look(textScale = TextScale.Larger),
            // Поля раздела и место под полосу прокрутки — те же, что
            // на экране: от них зависит, сколько ширины достаётся столбцам.
            content = {
                Column(modifier = Modifier.fillMaxSize().padding(Spacing.screen)) {
                    Column(modifier = Modifier.padding(end = Spacing.normal)) {
                        JournalRow(entry = entry, striped = true)
                    }
                }
            }
        ).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        val image = ImageIO.read(ByteArrayInputStream(frame))
        val background = image.getRGB(0, image.height - 1)
        return (0 until image.height).count { y -> image.getRGB(ROW_PROBE_X, y) != background }
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
        val TEXTS = journalTexts(Language.Ru).history

        /** Лента чека: длинная и узкая, как её рисует узел. */
        const val TAPE_WIDTH = 384
        const val TAPE_HEIGHT = 1200
        const val SETTLE = 40

        /** Надпись значка «мельче» в шапке окна просмотра. */
        val ZOOM_OUT = stringsOf(Language.Ru).preview.zoomOut
        const val NARROWING = 3

        /** Окно кассы на ноутбуке: на нём столбцы тесны и без крупного шрифта. */
        const val NARROW_WINDOW = 1000

        /** Точка, по которой меряется подложка ряда: внутри полей раздела. */
        const val ROW_PROBE_X = 20
    }
}
