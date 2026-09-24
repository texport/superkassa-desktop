package kz.mybrain.superkassa.presentation

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.cabinet.documents.receiptRow
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.document.select
import kz.mybrain.superkassa.presentation.journal.documents.journalEntriesOf
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Один и тот же чек находится и у кассы, и в кабинете одним поиском.
 *
 * Журнал кассы и документы кабинета — разные области с одной таблицей
 * журнала: поиск по ней обязан находить чек в обоих источниках.
 */
class JournalSourcesSearchTest {

    private val texts = textsOf(Language.Ru).common

    private val cabinet = textsOf(Language.Ru).cabinet

    private val names = mapOf("SALE" to TrilingualMessageResponse("Продажа", "Сатылым", "Sale"))

    /** Строки журнала кассы на русском. */
    private fun entries(vararg documents: FiscalDocumentResponse) =
        journalEntriesOf(texts, Language.Ru, names, documents.toList())

    /** Документ кассы: вид, состояние доставки и то, что проверке важно. */
    private fun document(
        id: String,
        status: String? = "SENT",
        type: String = "SALE",
        amount: Long? = null
    ) = CoreScene.document(id, type = type, amount = amount, status = status)

    @Test
    fun `один и тот же чек находится и у кассы, и в кабинете одним поиском`() {
        val document = document("d-1", amount = 450_084).copy(docNo = 12, createdAt = 1_788_807_779_000, shiftNo = 3)
        val receipt = CabinetReceipt(
            transactionId = "t-1",
            receiptNumber = "12",
            shiftNumber = 3,
            operationType = "SALE",
            total = Decimal.parse("4500.84"),
            createdAt = "2026-09-07T19:02:59Z",
            deliveryStatus = "ONLINE_OK"
        )
        val query = JournalQuery(search = "4500.84", shiftNo = 3)

        assertEquals(1, entries(document).select(query).size)
        assertEquals(1, listOf(receiptRow(receipt, cabinet).entry).select(query).size)
    }
}
