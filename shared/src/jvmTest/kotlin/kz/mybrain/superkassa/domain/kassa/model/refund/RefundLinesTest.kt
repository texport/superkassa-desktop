package kz.mybrain.superkassa.domain.kassa.model.refund

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Возврат по отметкам: строки чека-основания и их доли.
 *
 * Скидка на весь проданный чек делала строки дороже итога: три строки
 * по своим суммам давали тысячу при основании в девятьсот, и вернуть
 * все отмеченные строки было нельзя — касса отвечала «больше чека».
 */
class RefundLinesTest {

    private fun basis(total: Long) =
        CoreScene.document("d-42", amount = total, status = "SENT").copy(docNo = 42, createdAt = 1_700_000_000_000)

    private fun item(name: String, sum: String, storno: Boolean = false) = ReceiptItemView(
        name = name,
        price = Decimal.parse(sum),
        quantityThousandths = 1_000,
        sum = Decimal.parse(sum),
        isStorno = storno
    )

    private val items = listOf(item("Баранина", "500.00"), item("Коньяк", "300.00"), item("Хлеб", "200.00"))

    @Test
    fun `скидка на чек делится по строкам, и все строки дают итог основания`() {
        assertEquals(listOf(45_000L, 27_000L, 18_000L), refundShares(items, 90_000))

        val all = (0..2).fold(
            RefundDraft(basis(90_000), items = items, itemsRead = true)
        ) { draft, at -> draft.toggle(at) }

        assertEquals(RefundAmount.Ready(90_000), all.checked, "все строки основания вернуть нельзя")
        assertTrue(all.byLines)
    }

    @Test
    fun `остаток деления до тиына не теряется и не удваивается`() {
        val even = listOf(item("А", "100.00"), item("Б", "100.00"), item("В", "100.00"))

        val shares = refundShares(even, 20_000)

        assertEquals(listOf(6_667L, 6_667L, 6_666L), shares)
        assertEquals(20_000L, shares.sum())
    }

    @Test
    fun `без скидки на чек строка стоит своей суммы, сторно — ничего`() {
        val withStorno = items + item("Сок", "150.00", storno = true)

        assertEquals(listOf(50_000L, 30_000L, 20_000L, 0L), refundShares(withStorno, 100_000))
    }

    /** Строки чека возврата стоят ровно своей доли — столько и примет касса. */
    @Test
    fun `отмеченные строки уходят в чек со скидкой до своей доли`() {
        val draft = RefundDraft(basis(90_000), items = items, itemsRead = true).toggle(0).toggle(2)
        val plan = RefundPlan(
            kind = ReturnKind.Sell,
            basis = draft.basis,
            kgdKkmId = "123456789012",
            refundTiyn = draft.readyTiyn,
            lines = draft.chosen.sorted().map { draft.items[it] to draft.shares[it] },
            lineName = "Возврат по чеку № 42",
            payments = draft.split.toPayments(draft.readyTiyn),
            domain = DomainKind.Trading.plain
        )

        val lines = requireNotNull(plan.command("kkm-1", "1234", "key-1")).items
        val kassaSums = lines.map { Tenge.lineSum(Tenge.of(it.price), it.quantity) - it.discountOf() }

        assertEquals(listOf("Баранина", "Хлеб"), lines.map { it.name })
        assertEquals(tenge("630.00"), kassaSums.sum())
        assertEquals(tenge("630.00"), draft.readyTiyn, "оплата и строки описывают одну сумму")
    }

    /**
     * Поправленная после отметок сумма уходит одной строкой: чек описывал
     * бы строками восемь тысяч, а оплатой пять, и принять его касса не может.
     */
    @Test
    fun `поправленная сумма отменяет строки`() {
        val draft = RefundDraft(basis(100_000), items = items, itemsRead = true).toggle(0).toggle(1)
        assertTrue(draft.byLines)
        assertEquals("800,00", draft.entered)

        assertFalse(draft.copy(entered = "500").byLines, "поправленная сумма ушла бы строками на восемьсот")
    }

    private fun ReceiptItemRequest.discountOf() =
        discountSum?.let(Tenge::of) ?: 0L

    /**
     * Чек-основание ищется по номеру, напечатанному на бумажном чеке.
     *
     * Совпадение по вхождению: кассир набирает последние цифры, а не
     * переписывает номер целиком. Пустой набор ничего не отсеивает —
     * иначе список пропадал бы до первой набранной цифры.
     */
    @Test
    fun `основание отбирается по номеру чека`() {
        val document = basis(1000).copy(docNo = 100042)

        assertTrue(document.matches(""))
        assertTrue(document.matches("  "))
        assertTrue(document.matches("100042"))
        assertTrue(document.matches("0042"))
        assertTrue(!document.matches("77"))
        assertTrue(!CoreScene.document("d2").matches("1"))
        // Номер на бумаге — тот, что присвоила касса: по нему кассир
        // и ищет основание. По фискальному признаку тоже: он на чеке
        // стоит рядом.
        val printed = basis(1000).copy(docNo = 852804071, printedDocumentNumber = 5, fiscalSign = "852804071")
        assertTrue(printed.matches("5"))
        assertTrue(printed.matches("8528"))
        assertTrue(!printed.matches("3"))
    }
}
