package kz.mybrain.superkassa.desktop

import java.math.BigDecimal
import kz.mybrain.superkassa.desktop.server.ReceiptPayment
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.ui.returns.RefundAmount
import kz.mybrain.superkassa.desktop.ui.returns.RefundProblem
import kz.mybrain.superkassa.desktop.ui.returns.ReturnKind
import kz.mybrain.superkassa.desktop.ui.returns.drawerShortage
import kz.mybrain.superkassa.desktop.ui.returns.matches
import kz.mybrain.superkassa.desktop.ui.returns.refundAmountOf
import kz.mybrain.superkassa.desktop.ui.returns.refundLineName
import kz.mybrain.superkassa.desktop.ui.returns.refundRequest
import kz.mybrain.superkassa.desktop.ui.returns.tengeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Возврат: сумма и чек-основание.
 *
 * Ошибка здесь стоит денег в ящике, а не неудобства: вернуть больше, чем
 * было в чеке, или оформить возврат по возврату — расхождение с ОФД.
 */
class JournalRefundTest {

    private fun sale(number: Long = 12, total: Long? = 150_000, type: String = "SALE") = Document(
        id = "d-$number",
        docNo = number,
        docType = type,
        totalAmount = total,
        createdAt = 1_700_000_000_000
    )

    @Test
    fun `строка чека возврата названа номером с бумаги покупателя`() {
        // Номер от БФД с бумажным не совпадает: по нему покупатель свой
        // чек не опознает, а весь остальной экран возврата называет
        // основание бумажным номером.
        val basis = sale(number = 900_041).copy(printedDocumentNumber = 41)

        assertEquals("Возврат по чеку № 41", refundLineName("Возврат по чеку №", basis))
    }

    @Test
    fun `часть чека вернуть можно, а больше чека — нельзя`() {
        val total = 150_000L

        assertEquals(RefundAmount.Ready(50_000), refundAmountOf("500", total))
        assertEquals(RefundAmount.Ready(150_000), refundAmountOf("1500,00", total))
        assertEquals(RefundAmount.Ready(1), refundAmountOf("0,01", total))
        assertEquals(
            RefundAmount.Rejected(RefundProblem.TooLarge),
            refundAmountOf("1500,01", total),
            "возврат на тиын больше чека уже расходится с ОФД"
        )
    }

    @Test
    fun `пустая, ненулевая и отрицательная сумма названы по причине`() {
        assertEquals(RefundAmount.Rejected(RefundProblem.Empty), refundAmountOf("   ", 100))
        assertEquals(RefundAmount.Rejected(RefundProblem.NotANumber), refundAmountOf("пятьсот", 100))
        assertEquals(RefundAmount.Rejected(RefundProblem.NotPositive), refundAmountOf("0", 100))
        assertEquals(RefundAmount.Rejected(RefundProblem.NotPositive), refundAmountOf("-1", 100))
    }

    @Test
    fun `поле заполняется суммой чека в виде, который сам же и принимает`() {
        val text = tengeText(150_000)

        assertEquals(RefundAmount.Ready(150_000), refundAmountOf(text, 150_000))
    }

    @Test
    fun `в чеке возврата сумма своя, а реквизиты — чека-основания`() {
        val basis = sale(number = 42, total = 150_000)

        val request = refundRequest(
            basis = basis,
            kgdKkmId = "123456789012",
            refundTiyn = 50_000,
            idempotencyKey = "key-1",
            lineName = "Возврат по чеку № 42",
            payments = listOf(ReceiptPayment("CARD", BigDecimal("500")))
        )

        val parent = requireNonNull(request?.parentTicket)
        assertEquals(0, BigDecimal("500").compareTo(request?.items?.single()?.price), "возвращается указанная часть, а не весь чек")
        assertEquals(0, BigDecimal("500").compareTo(request?.payments?.single()?.sum))
        assertEquals("CARD", request?.payments?.single()?.type)
        assertEquals(42, parent.parentTicketNumber)
        assertEquals(0, BigDecimal("1500").compareTo(parent.parentTicketTotal), "реквизит чека-основания — его собственная сумма")
        assertNull(request?.items?.single()?.vatGroup, "ставку берёт касса: своя врала бы на кассе без НДС")
    }

    /**
     * Строки чека и оплата описывают одну и ту же сумму.
     *
     * Кассир отмечает позиции, поле заполняется их суммой — и поправить
     * её он вправе. Прежде отмеченные позиции уходили в ОФД своими
     * строками при любой набранной сумме: чек описывал строками десять
     * тысяч, а оплатой пять, и принять такой чек ОФД не может.
     */
    @Test
    fun `позиции уходят строками чека только вместе со своей суммой`() {
        val basis = sale(number = 42, total = 150_000)
        val returned = listOf(
            SoldItem(name = "Баранина", price = BigDecimal("500.00"), quantityThousandths = 1_000, sum = BigDecimal("500.00")),
            SoldItem(name = "Коньяк", price = BigDecimal("300.00"), quantityThousandths = 1_000, sum = BigDecimal("300.00"))
        )

        val matching = refundRequest(
            basis = basis,
            kgdKkmId = "123456789012",
            refundTiyn = 80_000,
            idempotencyKey = "key-1",
            lineName = "Возврат по чеку № 42",
            payments = listOf(ReceiptPayment("CASH", BigDecimal("800"))),
            returned = returned
        )
        val edited = refundRequest(
            basis = basis,
            kgdKkmId = "123456789012",
            refundTiyn = 50_000,
            idempotencyKey = "key-2",
            lineName = "Возврат по чеку № 42",
            payments = listOf(ReceiptPayment("CASH", BigDecimal("500"))),
            returned = returned
        )

        assertEquals(listOf("Баранина", "Коньяк"), matching?.items?.map { it.name })
        assertEquals(
            listOf("Возврат по чеку № 42"),
            edited?.items?.map { it.name },
            "поправленная сумма отправляла позиции на восемь тысяч с оплатой на пять"
        )
        assertEquals(0, BigDecimal("500").compareTo(edited?.items?.single()?.price))
    }

    @Test
    fun `возврат по возврату не предлагается ни при каком условии`() {
        val documents = listOf(
            sale(number = 1, type = "SALE"),
            sale(number = 2, type = "RETURN"),
            sale(number = 3, type = "BUY"),
            sale(number = 4, type = "BUY_RETURN")
        )

        assertEquals(listOf(1L), ReturnKind.Sell.basisIn(documents).map { it.docNo })
        assertEquals(listOf(3L), ReturnKind.Buy.basisIn(documents).map { it.docNo })
    }

    @Test
    fun `снятый с учёта тип CHECK в основание не берётся`() {
        val documents = listOf(sale(number = 7, type = "CHECK"))

        assertTrue(
            ReturnKind.Sell.basisIn(documents).isEmpty(),
            "CHECK не различал продажу и покупку: принять его значит вернуть покупку как продажу"
        )
        assertTrue(ReturnKind.Buy.basisIn(documents).isEmpty())
    }

    /**
     * Отвергнутый ОФД чек фискальным не стал, и возврата по нему не будет.
     *
     * Такой чек стоял в списке оснований наравне с проведёнными, и кассир,
     * выбрав его, отдавал деньги покупателю под чек возврата, который ОФД
     * отвергнет следом за основанием.
     */
    @Test
    fun `отвергнутый ОФД чек в основание не берётся`() {
        val refusedByStatus = sale(number = 8).copy(ofdStatus = "FAILED")
        val refusedByCode = sale(number = 9).copy(ofdErrorCode = 409)
        val queued = sale(number = 10).copy(ofdStatus = "OFFLINE_QUEUED", isAutonomous = true)

        assertEquals(
            listOf(10L),
            ReturnKind.Sell.basisIn(listOf(refusedByStatus, refusedByCode, queued)).map { it.docNo },
            "автономный чек фискальный и в основание годится, а отвергнутого ОФД нет вовсе"
        )
    }

    @Test
    fun `чек без номера или без суммы в основание не годится`() {
        val documents = listOf(
            Document(id = "a", docType = "SALE", totalAmount = 100),
            sale(number = 5, total = null)
        )

        assertTrue(
            ReturnKind.Sell.basisIn(documents).isEmpty(),
            "без номера и суммы чек-основание не собрать, а кнопка молчала бы"
        )
    }

    /**
     * Наличных в ящике меньше, чем отдают покупателю.
     *
     * Возврат продажи берёт деньги из того же ящика, из которого их
     * изымают: изъятие сверх остатка касса не проводила, а возврат той же
     * суммы отправляла молча — кассир называл покупателю сумму, которой
     * в ящике нет.
     */
    @Test
    fun `нехватка наличных на возврат названа до отправки`() {
        val drawer = 100_000L

        assertEquals(
            drawer,
            drawerShortage(ReturnKind.Sell, drawer, BigDecimal("1000.01")),
            "возврат продажи деньги из ящика отдаёт, и нехватку надо назвать"
        )
        assertNull(drawerShortage(ReturnKind.Sell, drawer, BigDecimal("1000.00")), "ровно остаток — хватает")
        assertNull(
            drawerShortage(ReturnKind.Buy, drawer, BigDecimal("5000.00")),
            "возврат покупки деньги принимает: ящику хватает всегда"
        )
        assertNull(
            drawerShortage(ReturnKind.Sell, null, BigDecimal("5000.00")),
            "неизвестный остаток о нехватке не свидетельствует"
        )
    }

    private fun <T : Any> requireNonNull(value: T?): T = requireNotNull(value)

    /**
     * Чек-основание ищется по номеру, напечатанному на бумажном чеке.
     *
     * Совпадение по вхождению: кассир набирает последние цифры, а не
     * переписывает номер целиком. Пустой набор ничего не отсеивает —
     * иначе список пропадал бы до первой набранной цифры.
     */
    @Test
    fun `основание отбирается по номеру чека`() {
        val document = Document(id = "d1", docNo = 100042, totalAmount = 1000)

        assertTrue(document.matches(""))
        assertTrue(document.matches("  "))
        assertTrue(document.matches("100042"))
        assertTrue(document.matches("0042"))
        assertTrue(!document.matches("77"))
        assertTrue(!Document(id = "d2").matches("1"))
        // Номер на бумаге — тот, что присвоила касса: по нему кассир
        // и ищет основание. По фискальному признаку тоже: он на чеке
        // стоит рядом.
        val printed = Document(id = "d3", docNo = 852804071, printedDocumentNumber = 5, fiscalSign = "852804071")
        assertTrue(printed.matches("5"))
        assertTrue(printed.matches("8528"))
        assertTrue(!printed.matches("3"))
    }
}
