package kz.mybrain.superkassa.presentation.common.document

import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Коды доставки читаются одним справочником везде, а не только на плашке.
 *
 * Доставку отмечают двумя наборами кодов: журнал документов отдаёт `SENT`
 * и `FAILED`, а справочник состояний отправки — `ONLINE_OK`, `ONLINE_ERROR`,
 * `OFFLINE_QUEUED` и `NOT_SENT`. Знавший по одному коду из каждой пары
 * принимал документ со вторым кодом за недоставленный, а очередь — за отказ.
 */
class DeliveryCodeReadingTest {

    /** Документ кассы читается одним справочником для обоих наборов. */
    @Test
    fun `документ кассы разбирает оба набора кодов`() {
        fun of(status: String) = deliveryOf(CoreScene.document("d-1", status = status))

        assertEquals(JournalDelivery.Delivered, of("SENT"))
        assertEquals(JournalDelivery.Delivered, of("ONLINE_OK"))
        assertEquals(JournalDelivery.Queued, of("PENDING"))
        assertEquals(JournalDelivery.Queued, of("OFFLINE_QUEUED"))
        assertEquals(JournalDelivery.Refused, of("FAILED"))
        assertEquals(JournalDelivery.Refused, of("ONLINE_ERROR"))
        assertEquals(JournalDelivery.Internal, of("INTERNAL"), "изъятие в Z-отчёте в БФД не уходит")
    }
}
