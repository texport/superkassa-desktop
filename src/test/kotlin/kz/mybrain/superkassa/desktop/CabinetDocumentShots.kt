package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocumentsScreen
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки документов кассы по данным БФД.
 *
 * Раздел показывает не то, что лежит в узле, а то, что приняла БФД,
 * и владелец приходит сюда за отметкой КГД. Смотрят здесь на четыре
 * вещи: объясняет ли себя пустой срок, держится ли столбец сумм
 * на сотне строк, видно ли чек без отметки КГД и что стоит на месте
 * документа, который кабинет не отдал.
 */
class CabinetDocumentShots {

    private val register = CabinetRegister(
        id = "r-1",
        kkmId = 5_000_021,
        internalName = "Касса у входа",
        status = "REGISTERED",
        registrationNumber = "000000010001"
    )

    private fun stage(receipts: String, opened: CabinetReply = CabinetReply("{}")) = CabinetStage { path ->
        when {
            path.endsWith("/documents") -> CabinetReply(CabinetBodies.OVERVIEW)
            path.endsWith("/receipts/search") -> CabinetReply(receipts)
            path.contains("/receipts/") -> opened
            path.contains("/shifts") -> CabinetReply(CabinetBodies.SHIFTS)
            path.contains("/reports") -> CabinetReply(CabinetBodies.REPORTS)
            path.contains("/cash-movements") -> CabinetReply(CabinetBodies.MOVEMENTS)
            else -> CabinetReply(CabinetBodies.NOTHING)
        }
    }

    private fun page(name: String, receipts: String, opened: CabinetReply = CabinetReply("{}")): ByteArray {
        val stage = stage(receipts, opened)
        return shot(name) {
            CabinetDocumentsScreen(stage.session, stage.cabinet, stage.texts, register)
        }
    }

    @Test
    fun `пустой срок объясняет себя, а сотня чеков держит столбец сумм`() {
        val empty = page("documents-empty", CabinetBodies.NOTHING)
        val many = page("documents-hundred", hundredReceipts())

        assertTrue(empty.isNotEmpty() && many.isNotEmpty())
        assertTrue(!empty.contentEquals(many), "пустой срок и сотня чеков выглядят одинаково")
    }

    /**
     * Чек с отметкой КГД и чек без неё.
     *
     * Отметка — то, ради чего чек и открывают: она значит, что чек дошёл
     * до БФД. Её отсутствие обязано читаться как отсутствие, а не как
     * пустое место в карточке. Чек раскрывается нажатием на строку —
     * иначе снимается список, а не карточка.
     */
    @Test
    fun `отметка КГД видна, а её отсутствие названо`() {
        val marked = opened("document-receipt-marked", CabinetReply(CabinetBodies.RECEIPT_MARKED))
        val plain = opened("document-receipt-unmarked", CabinetReply(CabinetBodies.RECEIPT_UNMARKED))

        assertTrue(marked.isNotEmpty() && plain.isNotEmpty())
        assertTrue(!marked.contentEquals(plain), "чек с отметкой КГД и чек без неё выглядят одинаково")
    }

    /** Раскрывает единственную строку списка и снимает то, что открылось. */
    private fun opened(name: String, receipt: CabinetReply): ByteArray {
        val stage = stage(CabinetBodies.ONE_RECEIPT, receipt)
        RenderProbe(content = {
            CabinetDocumentsScreen(stage.session, stage.cabinet, stage.texts, register)
        }).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(ROW_X, ROW_Y))
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/cabinet-$name.png").writeBytes(frame)
            return frame
        }
    }

    /**
     * Документ, которого кабинет не отдал.
     *
     * Кабинет отвечает 500, `openDocument` возвращает пустоту, и экран
     * обязан вернуть владельца к списку, а не оставить его с ожиданием
     * без конца.
     */
    @Test
    fun `отказ кабинета по документу не оставляет пустого ожидания`() {
        val refused = opened(
            "document-open-refused",
            refusal("INTERNAL", "Unexpected error", HttpStatusCode.InternalServerError)
        )
        val card = opened("document-receipt-marked", CabinetReply(CabinetBodies.RECEIPT_MARKED))

        assertTrue(refused.isNotEmpty())
        assertTrue(!refused.contentEquals(card), "отказ по документу открыл карточку")
    }

    /**
     * Остальные виды документов: смены, отчёты и движение денег.
     *
     * Вид выбирается сегментом, и у смен срока нет вовсе — ряд сроков
     * над списком обязан исчезнуть, а не обещать отбор, которого
     * кабинет не сделает. У открытой смены итогов ещё нет, и пустые
     * строки в её карточке не ошибка.
     */
    @Test
    fun `смены, отчёты и движение денег показываются каждый по-своему`() {
        val frames = mapOf(
            "documents-shifts" to byKind("documents-shifts", SHIFTS_X),
            "documents-reports" to byKind("documents-reports", REPORTS_X),
            "documents-cash" to byKind("documents-cash", CASH_X)
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(frames.values.map { it.toList() }.distinct().size == frames.size, "виды неотличимы")
    }

    /** Переключает вид документов нажатием на сегмент и снимает список. */
    private fun byKind(name: String, at: Float): ByteArray {
        val stage = stage(CabinetBodies.ONE_RECEIPT)
        RenderProbe(content = {
            CabinetDocumentsScreen(stage.session, stage.cabinet, stage.texts, register)
        }).use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(at, KIND_Y))
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/cabinet-$name.png").writeBytes(frame)
            return frame
        }
    }

    /**
     * Сотня чеков одной страницей.
     *
     * Суммы разной длины намеренно: столбец цифр держится денежным
     * шрифтом, и «1 200,00» рядом с «435,84» обязаны стоять друг
     * под другом.
     */
    private fun hundredReceipts(): String {
        val rows = (1..100).joinToString(",") { at ->
            val kind = if (at % 7 == 0) "RETURN" else "SALE"
            val mark = if (at % 3 == 0) "null" else "\"2100000000${"%02d".format(at)}\""
            """{"transactionId":"t-$at","receiptNumber":"$at","shiftNumber":${at / 20 + 1},
               "operationType":"$kind","total":${at * 137}.${"%02d".format(at % 100)},
               "createdAt":"2026-09-0${at % 7 + 1}T1${at % 10}:02:59Z",
               "deliveryStatus":"ONLINE_OK","kgdMark":$mark}"""
        }
        return """{"page":0,"size":100,"totalElements":100,"items":[$rows]}"""
    }

    private companion object {
        const val SETTLE = 30

        /** Единственная строка списка чеков: по ней и нажимаем. */
        const val ROW_X = 300f
        const val ROW_Y = 443f

        /** Ряд видов документов над списком: чеки, смены, отчёты, движение денег. */
        const val KIND_Y = 114f
        const val SHIFTS_X = 284f
        const val REPORTS_X = 465f
        const val CASH_X = 646f
    }
}
