package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.history.JournalDelivery
import kz.mybrain.superkassa.desktop.ui.history.JournalEntry
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalQuery
import kz.mybrain.superkassa.desktop.ui.history.JournalSort
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import kz.mybrain.superkassa.desktop.ui.history.deliveriesIn
import kz.mybrain.superkassa.desktop.ui.history.journalTypesIn
import kz.mybrain.superkassa.desktop.ui.history.presentIn
import kz.mybrain.superkassa.desktop.ui.history.select
import kz.mybrain.superkassa.desktop.ui.history.shiftsIn
import java.math.BigDecimal
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

    private fun entry(
        key: String,
        at: Long? = 0,
        type: String? = "SALE",
        number: String = "1",
        numberOrder: Long? = 1,
        amount: String = "0,00 ₸",
        amountOrder: BigDecimal? = BigDecimal.ZERO,
        sign: String = "—",
        delivery: JournalDelivery? = JournalDelivery.Delivered,
        shiftNo: Long? = 7,
        about: String = ""
    ) = JournalEntry(
        key = key,
        at = at,
        moment = "07.09 12:00:00",
        typeCode = type,
        type = type.orEmpty(),
        number = number,
        numberOrder = numberOrder,
        amount = amount,
        amountOrder = amountOrder,
        sign = sign,
        delivery = delivery,
        shiftNo = shiftNo,
        about = about
    )

    private val sale = entry(
        key = "sale",
        at = 300,
        number = "12",
        numberOrder = 12,
        amount = "4 500,00 ₸",
        amountOrder = BigDecimal("4500.00"),
        sign = "AB12CD34"
    )

    private val refund = entry(
        key = "refund",
        at = 200,
        type = "RETURN",
        number = "13",
        numberOrder = 13,
        amount = "950,00 ₸",
        amountOrder = BigDecimal("950.00"),
        delivery = JournalDelivery.Refused,
        shiftNo = 8
    )

    private val queued = entry(
        key = "queued",
        at = 100,
        number = "14",
        numberOrder = 14,
        amount = "1 200,00 ₸",
        amountOrder = BigDecimal("1200.00"),
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
        val unknown = entry(key = "unknown", at = null, numberOrder = null, amountOrder = null)
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
