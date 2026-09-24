package kz.mybrain.superkassa.domain.kassa.model.refund

import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest
import kz.mybrain.superkassa.domain.kassa.model.BuyerContact
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Контакт покупателя в чеке возврата: по нему ему уходит чек, как и при продаже. */
class RefundContactTest {

    private val basis = CoreScene.document("d-42", amount = 90_000, status = "SENT").copy(docNo = 42)

    private fun command(draft: RefundDraft) = assertNotNull(
        draft.plan(ReturnKind.Sell, "000000200042", "Возврат", DomainKind.Trading.plain).command("kkm-1", "4826", "k")
    )

    @Test
    fun `почта покупателя уходит в чек возврата`() {
        val draft = RefundDraft(basis, contact = BuyerContact(ContactKind.Email, "buyer@example.kz"))

        assertTrue(draft.ready)
        assertEquals(CustomerContactRequest(email = "buyer@example.kz"), command(draft).customerContact)
    }

    @Test
    fun `без контакта чек возврата покупателю не уходит`() {
        assertNull(command(RefundDraft(basis)).customerContact)
    }

    @Test
    fun `контакт с ошибкой возврат не оформляет`() {
        assertFalse(RefundDraft(basis, contact = BuyerContact(ContactKind.Phone, "8 701")).ready)
    }
}
