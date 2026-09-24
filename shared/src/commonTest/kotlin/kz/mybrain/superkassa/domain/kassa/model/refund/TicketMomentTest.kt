package kz.mybrain.superkassa.domain.kassa.model.refund

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** Время чека-основания — поле протокола БФД, а не показ: вид задан, пояс нулевой. */
class TicketMomentTest {

    @Test
    fun `время чека-основания уходит в БФД по UTC`() {
        val moment = Instant.parse("2026-09-07T14:42:08Z").toEpochMilliseconds()
        assertEquals("2026-09-07T14:42:08", ticketMoment(moment))
    }
}
