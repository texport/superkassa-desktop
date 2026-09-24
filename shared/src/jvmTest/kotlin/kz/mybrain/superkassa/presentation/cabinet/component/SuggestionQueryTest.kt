package kz.mybrain.superkassa.presentation.cabinet.component

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Когда кабинет спрашивают о подсказках: по пустому запросу и от двух букв. */
class SuggestionQueryTest {

    /** Подсказки кабинета: пустой запрос принимается, одна буква — нет. */
    @Test
    fun `suggestions need either nothing or two letters`() {
        assertTrue(askableQuery(""))
        assertFalse(askableQuery("а"))
        assertTrue(askableQuery("ал"))
    }
}
