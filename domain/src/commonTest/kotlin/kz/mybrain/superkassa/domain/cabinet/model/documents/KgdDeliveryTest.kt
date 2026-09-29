package kz.mybrain.superkassa.domain.cabinet.model.documents

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Состояние документа кабинета в КГД — по каждой строке правила.
 *
 * Прежде у всех документов стояло «Принят»: передача службе, везущей
 * документ в КГД, принималась за приём самим КГД.
 */
class KgdDeliveryTest {

    @Test
    fun итогВКгдВажнееПередачи() {
        assertEquals(KgdDelivery.Accepted, KgdDelivery.ofReceipt("DELIVERED", "ACCEPTED"))
        assertEquals(KgdDelivery.Rejected, KgdDelivery.ofReceipt("REJECTED", "ACCEPTED"))
        assertEquals(KgdDelivery.Failed, KgdDelivery.ofReceipt("FAILED", "ACCEPTED"))
        assertEquals(KgdDelivery.Sent, KgdDelivery.ofReceipt("SENT", "ACCEPTED"))
    }

    @Test
    fun безИтогаГоворитПередача() {
        assertEquals(KgdDelivery.Transferring, KgdDelivery.ofReceipt(null, "IN_PROGRESS"))
        assertEquals(KgdDelivery.TransferFailed, KgdDelivery.ofReceipt("", "FAILED"))
        assertEquals(KgdDelivery.Awaiting, KgdDelivery.ofReceipt(null, "ACCEPTED"))
        assertEquals(KgdDelivery.Awaiting, KgdDelivery.ofReceipt(null, null))
    }

    @Test
    fun вКгдУходятТолькоЧекиИZОтчёт() {
        assertEquals(KgdDelivery.Accepted, KgdDelivery.ofReport("Z", "DELIVERED", "ACCEPTED"))
        assertEquals(KgdDelivery.Rejected, KgdDelivery.ofReport("z", "REJECTED", null))
        // X-отчёт и отчёт незнакомого вида в КГД не передаются, что бы ни пришло в полях.
        listOf("X", "UNKNOWN", null).forEach { type ->
            assertEquals(KgdDelivery.NotSent, KgdDelivery.ofReport(type, "DELIVERED", "ACCEPTED"), "$type")
        }
        assertEquals(KgdDelivery.NotSent, KgdDelivery.ofMovement())
    }

    @Test
    fun отказыКрасные() {
        val refused = KgdDelivery.entries.filter { it.tone == KgdTone.Refused }
        assertEquals(setOf(KgdDelivery.Rejected, KgdDelivery.Failed, KgdDelivery.TransferFailed), refused.toSet())
        assertEquals(KgdTone.Done, KgdDelivery.Accepted.tone)
        assertEquals(KgdTone.Neutral, KgdDelivery.NotSent.tone)
    }
}
