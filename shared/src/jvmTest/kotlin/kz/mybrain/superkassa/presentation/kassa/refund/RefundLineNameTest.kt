package kz.mybrain.superkassa.presentation.kassa.refund

import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals

/** Строка чека возврата: как основание названо покупателю. */
class RefundLineNameTest {

    @Test
    fun `строка чека возврата названа номером с бумаги покупателя`() {
        // Номер от БФД с бумажным не совпадает: по нему покупатель свой
        // чек не опознает, а весь остальной экран возврата называет
        // основание бумажным номером.
        val basis = CoreScene.document("d-900041", type = "SALE", amount = 150_000, status = "SENT")
            .copy(docNo = 900_041, createdAt = 1_700_000_000_000, printedDocumentNumber = 41)

        assertEquals("Возврат по чеку № 41", refundLineName("Возврат по чеку №", RefundDraft(basis)))
    }
}
