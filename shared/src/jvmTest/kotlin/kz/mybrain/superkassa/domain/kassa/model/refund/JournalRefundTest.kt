package kz.mybrain.superkassa.domain.kassa.model.refund

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.kassa.refund.refundLineName
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

    private fun sale(number: Long = 12, total: Long? = 150_000, type: String = "SALE") =
        CoreScene.document("d-$number", type = type, amount = total, status = "SENT")
            .copy(docNo = number, createdAt = 1_700_000_000_000)

    @Test
    fun `строка чека возврата названа номером с бумаги покупателя`() {
        // Номер от БФД с бумажным не совпадает: по нему покупатель свой
        // чек не опознает, а весь остальной экран возврата называет
        // основание бумажным номером.
        val basis = sale(number = 900_041).copy(printedDocumentNumber = 41)

        assertEquals("Возврат по чеку № 41", refundLineName("Возврат по чеку №", RefundDraft(basis)))
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
        val text = Tenge.entered(150_000)

        assertEquals(RefundAmount.Ready(150_000), refundAmountOf(text, 150_000))
    }

    /** Возврат суммой: 500 ₸ картой по чеку на 1 500 ₸ с кассы такси. */
    private fun refundBySum() = requireNotNull(
        RefundPlan(
            kind = ReturnKind.Sell,
            basis = sale(number = 42, total = 150_000),
            kgdKkmId = "123456789012",
            refundTiyn = 50_000,
            lines = emptyList(),
            lineName = "Возврат по чеку № 42",
            payments = listOf(ReceiptPaymentRequest("CARD", Decimal.parse("500"))),
            domain = DomainKind.Taxi.plain
        ).command("kkm-1", "1234", "key-1")
    )

    @Test
    fun `в чеке возврата сумма своя`() {
        val command = refundBySum()
        val line = command.items.single()

        assertEquals(0, tenge("500").compareTo(line.price.let(Tenge::of)), "возвращается часть, а не весь чек")
        assertEquals(0, tenge("500").compareTo(command.payments.single().sum.let(Tenge::of)))
        assertEquals("CARD", command.payments.single().type)
        assertEquals("SELL_RETURN", command.operation)
        assertNull(line.vatGroup, "ставку берёт касса у основания: своя врала бы на кассе без НДС")
    }

    @Test
    fun `реквизиты в чеке возврата — чека-основания`() {
        val command = refundBySum()
        val parent = requireNotNull(command.parentTicket)

        assertEquals(42, parent.parentTicketNumber)
        assertEquals(0, tenge("1500").compareTo(parent.parentTicketTotal.let(Tenge::of)), "не сумма основания")
        // Вид отрасли протокол требует у каждого чека: у возврата он тот же,
        // в котором работает касса, а реквизиты поездки принадлежат основанию.
        assertEquals("DOMAIN_TAXI", command.domain?.type, "возврат ушёл бы торговлей с кассы такси")
        assertNull(command.domain?.taxi, "возврат унёс бы выдуманный номер машины")
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
            CoreScene.document("a", amount = 100),
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
            drawerShortage(ReturnKind.Sell, drawer, tenge("1000.01")),
            "возврат продажи деньги из ящика отдаёт, и нехватку надо назвать"
        )
        assertNull(drawerShortage(ReturnKind.Sell, drawer, tenge("1000.00")), "ровно остаток — хватает")
        assertNull(
            drawerShortage(ReturnKind.Buy, drawer, tenge("5000.00")),
            "возврат покупки деньги принимает: ящику хватает всегда"
        )
        assertNull(
            drawerShortage(ReturnKind.Sell, null, tenge("5000.00")),
            "неизвестный остаток о нехватке не свидетельствует"
        )
    }
}
