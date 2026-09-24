package kz.mybrain.superkassa.domain.print.model

import kz.mybrain.superkassa.domain.print.model.PinRefusal.Next
import kotlin.test.Test
import kotlin.test.assertEquals

/** Что делать с пином печатной формы после отказа — по коду отказа, а не по факту. */
class PinRefusalTest {

    @Test
    fun `неверный пин спрашивается заново`() {
        listOf("USER_NOT_FOUND", "USER_FORBIDDEN", "FORBIDDEN").forEach {
            assertEquals(Next.AskAgain, PinRefusal.next(it), it)
        }
    }

    @Test
    fun `заблокированный пин забывается без нового вопроса`() {
        assertEquals(Next.Forget, PinRefusal.next("PIN_LOCKED"))
    }

    @Test
    fun `отказ не по пину пин не трогает`() {
        assertEquals(Next.Keep, PinRefusal.next("DOCUMENT_NOT_FOUND"))
    }
}
