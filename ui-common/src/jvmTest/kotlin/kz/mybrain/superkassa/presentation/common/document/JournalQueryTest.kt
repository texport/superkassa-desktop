package kz.mybrain.superkassa.presentation.common.document

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Поиск, отбор и порядок строк журнала.
 *
 * Отбор один на два экрана: журнал кассы и документы кассы в кабинете.
 * Поэтому проверяется он как предмет, а не через экраны: чек, найденный
 * кассиром у кассы, владелец обязан найти в кабинете тем же поиском —
 * «нашёлся здесь, не нашёлся там» худшее, что может сделать журнал.
 */
class JournalQueryTest {

    /** Строка журнала, от которой строки проверки отличаются только нужным. */
    private val blank = JournalEntry(
        key = "",
        at = 0,
        moment = "07.09 12:00:00",
        typeCode = "SALE",
        type = "SALE",
        number = "1",
        numberOrder = 1,
        amount = "0,00 ₸",
        amountOrder = Decimal.ZERO,
        sign = "—",
        delivery = JournalDelivery.Delivered,
        shiftNo = 7
    )

    private val sale = blank.copy(
        key = "sale",
        at = 300,
        number = "12",
        numberOrder = 12,
        amount = "4 500,00 ₸",
        amountOrder = Decimal.parse("4500.00"),
        sign = "AB12CD34"
    )

    private val refund = blank.copy(
        key = "refund",
        at = 200,
        typeCode = "RETURN",
        type = "RETURN",
        number = "13",
        numberOrder = 13,
        amount = "950,00 ₸",
        amountOrder = Decimal.parse("950.00"),
        delivery = JournalDelivery.Refused,
        shiftNo = 8
    )

    private val queued = blank.copy(
        key = "queued",
        at = 100,
        number = "14",
        numberOrder = 14,
        amount = "1 200,00 ₸",
        amountOrder = Decimal.parse("1200.00"),
        delivery = JournalDelivery.Queued,
        about = "хлеб"
    )

    private val all = listOf(sale, refund, queued)

    @Test
    fun `сумма находится так, как её набирают — без разделителя разрядов`() {
        assertEquals(
            listOf("sale"),
            all.select(JournalQuery(search = "4500")).map { it.key },
            "«4 500,00 ₸» по «4500» не находилось бы подстрокой"
        )
    }

    @Test
    fun `сумма находится и с тиынами, набранными через запятую`() {
        // На экране и на чеке тиыны отделены запятой, а числом сумма
        // хранится с точкой: «4500,00» не находилось ни тем, ни другим.
        assertEquals(listOf("sale"), all.select(JournalQuery(search = "4500,00")).map { it.key })
        assertEquals(listOf("sale"), all.select(JournalQuery(search = "4500.00")).map { it.key })
    }

    @Test
    fun `поиск идёт по фискальному признаку и по номеру`() {
        assertEquals(listOf("sale"), all.select(JournalQuery(search = "ab12")).map { it.key })
        assertEquals(listOf("refund"), all.select(JournalQuery(search = "13")).map { it.key })
    }

    @Test
    fun `слова поиска сходятся по разным столбцам сразу`() {
        assertEquals(
            listOf("sale"),
            all.select(JournalQuery(search = "sale 4500")).map { it.key },
            "вид документа и сумма стоят в разных столбцах"
        )
        assertTrue(all.select(JournalQuery(search = "return 4500")).isEmpty())
    }

    @Test
    fun `поиск находит и то, чего в столбцах нет`() {
        assertEquals(
            listOf("queued"),
            all.select(JournalQuery(search = "хлеб")).map { it.key },
            "наименование позиции в столбцы не помещается, а искать по нему нужно"
        )
    }

    @Test
    fun `пустой поиск оставляет всё`() {
        assertEquals(3, all.select(JournalQuery(search = "   ")).size)
    }

    @Test
    fun `порядок по сумме считается числом, а не текстом`() {
        val byAmount = all.select(JournalQuery(sort = JournalSort.Amount, descending = true))
        assertEquals(
            listOf("sale", "queued", "refund"),
            byAmount.map { it.key },
            "по тексту «950,00 ₸» оказалось бы крупнее «4 500,00 ₸»"
        )
    }

    @Test
    fun `порядок переворачивается в обе стороны`() {
        assertEquals(listOf("sale", "refund", "queued"), all.select(JournalQuery()).map { it.key })
        assertEquals(
            listOf("queued", "refund", "sale"),
            all.select(JournalQuery(descending = false)).map { it.key }
        )
    }

    @Test
    fun `запись без значения не занимает начало обратного порядка`() {
        val unknown = blank.copy(key = "unknown", at = null, numberOrder = null, amountOrder = null)
        val ordered = (all + unknown).select(JournalQuery(sort = JournalSort.Number))
        assertEquals("unknown", ordered.last().key, "чек без номера уходит в конец, а не встаёт первым")
    }

    @Test
    fun `отбор по виду, состоянию и смене складывается`() {
        assertEquals(listOf("refund"), all.select(JournalQuery(type = "RETURN")).map { it.key })
        assertEquals(
            listOf("refund"),
            all.select(JournalQuery(delivery = JournalDelivery.Refused)).map { it.key }
        )
        assertEquals(listOf("sale", "queued"), all.select(JournalQuery(shiftNo = 7)).map { it.key })
        assertTrue(all.select(JournalQuery(type = "RETURN", shiftNo = 7)).isEmpty())
    }

    @Test
    fun `наборы отбора строятся по тому, что пришло`() {
        assertEquals(listOf("SALE", "RETURN"), journalTypesIn(all).map { it.code })
        assertEquals(listOf(8L, 7L), shiftsIn(all), "смены идут от последней к первой")
        assertEquals(
            listOf(JournalDelivery.Delivered, JournalDelivery.Queued, JournalDelivery.Refused),
            deliveriesIn(all)
        )
    }

    @Test
    fun `отбор, которого в пришедших строках нет, снимается сам`() {
        val stale = JournalQuery(type = "CASH_IN", delivery = JournalDelivery.Internal, shiftNo = 99)
        val narrowed = stale.presentIn(all)

        assertNull(narrowed.type, "пустой список без объяснения читается как утрата документов")
        assertNull(narrowed.delivery)
        assertNull(narrowed.shiftNo)
        assertEquals("RETURN", JournalQuery(type = "RETURN").presentIn(all).type)
    }

    @Test
    fun `отобранным журнал считает себя только по отбору, а не по порядку`() {
        assertTrue(!JournalQuery(sort = JournalSort.Amount).narrowed)
        assertTrue(JournalQuery(search = "12").narrowed)
        assertTrue(JournalQuery(shiftNo = 7).narrowed)
    }

    @Test
    fun `срок без границ не листается`() {
        val whole = JournalPeriod.of(JournalSpan.All)
        assertNull(whole.range)
        assertTrue(!whole.hasLater(), "у «всего времени» вперёд идти некуда")
        assertNull(whole.shiftedBy(-1).range)
    }
}
