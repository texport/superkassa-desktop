package kz.mybrain.superkassa.presentation.journal.documents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.journal.model.DocumentPages
import kz.mybrain.superkassa.presentation.common.document.JournalDelivery
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.document.select
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Журнал документов на настоящем ядре и тестовом БФД: что с документом
 * в БФД — принят, в очереди, отклонён с причиной, — отбор, поиск
 * и дочитывание срока страницами.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JournalCoreTest {
    private val scene = DeliveryBench(ordered = false)
    private val texts = textsOf(Language.Ru).common

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun close() {
        scene.close()
        Dispatchers.resetMain()
    }

    private fun week(): JournalViewModel = scene.model().apply { choose(JournalPeriod.of(JournalSpan.Week)) }

    private fun JournalViewModel.entries() =
        journalEntriesOf(texts, Language.Ru, state.value.documentTypes, state.value.documents)

    @Test
    fun `принят, в очереди и отклонён с причиной — каждый своим состоянием`() {
        val sale = scene.kassa.sell()
        val rejected = scene.kassa.rejectedSale()
        // Без связи касса уходит в автономный режим: следующий чек тоже встал бы в очередь.
        val offline = scene.kassa.offlineSale()
        val entries = week().entries().associateBy { it.key }

        assertEquals(JournalDelivery.Delivered, entries.getValue(sale.documentId).delivery)
        assertEquals(JournalDelivery.Queued, entries.getValue(offline.documentId).delivery)
        val refused = entries.getValue(rejected.documentId)
        assertEquals(JournalDelivery.Refused, refused.delivery)
        assertTrue(refused.refusal.orEmpty().contains("13"), "у отказа нет кода: ${refused.refusal}")
        assertFalse(refused.printable, "отклонённый чек предложено напечатать")
        assertFalse(refused.openable, "отклонённый чек открывает доставку")
        assertTrue(entries.getValue(sale.documentId).openable)
    }

    @Test
    fun `отбор по признаку и поиск по сумме находят документ`() {
        scene.kassa.sell(price = "4500.00", quantity = "1")
        scene.kassa.rejectedSale()
        val entries = week().entries()

        val refused = entries.select(JournalQuery(delivery = JournalDelivery.Refused))
        assertEquals(1, refused.size)
        val found = entries.select(JournalQuery(search = "4500"))
        assertEquals(1, found.size, "поиск по сумме без разделителя не нашёл чек")
        assertTrue(entries.none { it.type == "SALE" }, "вид документа показан кодом")
    }

    @Test
    fun `срок больше страницы дочитывается до конца, без повторов`() {
        repeat(MANY) { scene.kassa.cashIn("10.00") }
        val model = week()
        assertEquals(DocumentPages.DOCUMENTS, model.state.value.documents.size)
        assertTrue(model.state.value.page.more)

        while (model.state.value.page.more) model.more()

        val ids = model.state.value.documents.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "документ повторился при дочитывании")
        assertEquals(MANY + 1, ids.size, "прочитан не весь срок")
    }

    private companion object {
        /** Больше двух страниц журнала. */
        const val MANY = 450
    }
}
